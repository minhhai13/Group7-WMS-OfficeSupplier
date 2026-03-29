package com.minhhai.wms.service.impl;

import com.minhhai.wms.dao.GoodsIssueDetailDao;
import com.minhhai.wms.dao.GoodsIssueNoteDao;
import com.minhhai.wms.dao.PartnerDao;
import com.minhhai.wms.dao.ProductDao;
import com.minhhai.wms.dao.ProductUoMConversionDao;
import com.minhhai.wms.dao.PurchaseRequestDao;
import com.minhhai.wms.dao.PurchaseRequestDetailDao;
import com.minhhai.wms.dao.SalesOrderDao;
import com.minhhai.wms.dao.SalesOrderDetailDao;
import com.minhhai.wms.dao.StockBatchDao;
import com.minhhai.wms.dao.StockMovementDao;
import com.minhhai.wms.dto.SaleOrderDTO;
import com.minhhai.wms.dto.SaleOrderDetailDTO;
import com.minhhai.wms.dto.StockCheckResult;
import com.minhhai.wms.entity.*;
import com.minhhai.wms.service.SalesOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SalesOrderServiceImpl implements SalesOrderService {

    private final SalesOrderDao soDao;
    private final SalesOrderDetailDao soDetailDao;
    private final PartnerDao partnerDao;
    private final ProductDao productDao;
    private final ProductUoMConversionDao uomConversionDao;
    private final GoodsIssueNoteDao ginDao;
    private final GoodsIssueDetailDao ginDetailDao;
    private final StockBatchDao stockBatchDao;
    private final StockMovementDao stockMovementDao;
    private final PurchaseRequestDao prDao;
    private final PurchaseRequestDetailDao prDetailDao;

    // ==================== Query Methods ====================

    @Override
    @Transactional(readOnly = true)
    public List<SaleOrderDTO> getSOsByWarehouse(Integer warehouseId, String status, Integer customerId) {
        List<SalesOrder> orders;
        boolean hasStatus = status != null && !status.isBlank();
        boolean hasCustomer = customerId != null;

        if (hasStatus && hasCustomer) {
            orders = soDao.findByWarehouseIdAndStatusAndCustomerId(warehouseId, status, customerId);
        } else if (hasStatus) {
            orders = soDao.findByWarehouseIdAndStatus(warehouseId, status);
        } else if (hasCustomer) {
            orders = soDao.findByWarehouseIdAndCustomerId(warehouseId, customerId);
        } else {
            orders = soDao.findByWarehouseId(warehouseId);
        }
        return orders.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SaleOrderDTO getSOById(Integer soId) {
        SalesOrder so = soDao.findById(soId)
                .orElseThrow(() -> new IllegalArgumentException("Sales Order not found: " + soId));
        so.setDetails(soDetailDao.findBySoId(soId));
        return mapToDTO(so);
    }

    // ==================== Save Draft ====================

    @Override
    public SaleOrderDTO saveDraft(SaleOrderDTO dto, User currentUser) {
        SalesOrder so = buildSalesOrder(dto, currentUser);
        so.setSoStatus("Draft");
        so.setRejectReason(null);
        boolean isUpdate = dto.getSoId() != null;
        List<SalesOrderDetail> details = buildDetails(dto);
        so = saveSalesOrder(so, details, isUpdate);
        return mapToDTO(so);
    }

    // ==================== Submit with ATP Check ====================

    @Override
    public StockCheckResult checkAndSubmit(SaleOrderDTO dto, User currentUser, boolean createPR) {
        // 1. Validate all lines
        List<SaleOrderDetailDTO> allDetails = dto.getDetails();
        if (allDetails == null || allDetails.isEmpty()) {
            throw new IllegalArgumentException("At least one product line is required to submit.");
        }
        for (int i = 0; i < allDetails.size(); i++) {
            SaleOrderDetailDTO d = allDetails.get(i);
            if (d.getProductId() == null) throw new IllegalArgumentException("Product is required on line " + (i + 1) + ".");
            if (d.getUom() == null || d.getUom().isBlank()) throw new IllegalArgumentException("UoM is required on line " + (i + 1) + ".");
            if (d.getOrderedQty() == null || d.getOrderedQty() <= 0) throw new IllegalArgumentException("Quantity must be greater than 0 on line " + (i + 1) + ".");
        }

        // 2. Map to entity but keep as Draft for now (so mapToEntity won't block re-entry)
        SalesOrder so = buildSalesOrder(dto, currentUser);
        so.setSoStatus("Draft");           // ← Keep Draft until decision
        so.setRejectReason(null);
        boolean isUpdate = dto.getSoId() != null;
        List<SalesOrderDetail> details = buildDetails(dto);
        so = saveSalesOrder(so, details, isUpdate);

        Integer warehouseId = so.getWarehouse().getWarehouseId();

        // 3. ATP check for each product line
        List<StockCheckResult.ShortageItem> shortages = new ArrayList<>();
        for (SalesOrderDetail soDetail : so.getDetails()) {
            Product product = soDetail.getProduct();
            BigDecimal conversionFactor = getConversionFactor(product, soDetail.getUom());
            int baseQtyNeeded = BigDecimal.valueOf(soDetail.getOrderedQty())
                    .multiply(conversionFactor).intValue();

            // Total_ATP = Σ(qtyAvailable - qtyReserved) for this product in THIS warehouse
            List<StockBatch> batches = stockBatchDao
                    .findByWarehouseIdAndProductId(warehouseId, product.getProductId());
            int totalATP = 0;
            for (StockBatch batch : batches) {
                totalATP += (batch.getQtyAvailable() - batch.getQtyReserved());
            }

            if (baseQtyNeeded > totalATP) {
                int missingQty = baseQtyNeeded - totalATP;
                shortages.add(StockCheckResult.ShortageItem.builder()
                        .productName(product.getProductName())
                        .sku(product.getSku())
                        .orderedQty(baseQtyNeeded)
                        .availableQty(totalATP)
                        .missingQty(missingQty)
                        .uom(product.getBaseUoM())
                        .build());
            }
        }

        // 4. Handle shortages — defer status change until decision is final
        if (!shortages.isEmpty()) {
            if (!createPR) {
                // First call: SO stays Draft (user can re-submit with PR)
                return StockCheckResult.builder()
                        .success(true)
                        .hasShortage(true)
                        .soId(so.getSoId())
                        .shortages(shortages)
                        .build();
            } else {
                // Second call: confirmed → now set Pending Approval + create PR
                so.setSoStatus("Pending Approval");
                so = soDao.save(so);
                String prNumber = createPurchaseRequest(so);
                return StockCheckResult.builder()
                        .success(true)
                        .hasShortage(true)
                        .soId(so.getSoId())
                        .prNumber(prNumber)
                        .shortages(shortages)
                        .build();
            }
        }

        // 5. No shortage → set Pending Approval directly
        so.setSoStatus("Pending Approval");
        so = soDao.save(so);
        return StockCheckResult.builder()
                .success(true)
                .hasShortage(false)
                .soId(so.getSoId())
                .build();
    }

    // ==================== PR Creation ====================

    private String createPurchaseRequest(SalesOrder so) {
        String prNumber = generatePRNumber();

        PurchaseRequest pr = new PurchaseRequest();
        pr.setPrNumber(prNumber);
        pr.setWarehouse(so.getWarehouse());
        pr.setStatus("Pending");
        pr.setRelatedSalesOrder(so);
        pr.setDetails(new ArrayList<>());

        for (SalesOrderDetail soDetail : so.getDetails()) {
            Product product = soDetail.getProduct();
            BigDecimal conversionFactor = getConversionFactor(product, soDetail.getUom());
            int baseQtyNeeded = BigDecimal.valueOf(soDetail.getOrderedQty())
                    .multiply(conversionFactor).intValue();

            // Recalculate ATP for this product
            Integer warehouseId = so.getWarehouse().getWarehouseId();
            List<StockBatch> batches = stockBatchDao
                    .findByWarehouseIdAndProductId(warehouseId, product.getProductId());
            int totalATP = 0;
            for (StockBatch batch : batches) {
                totalATP += (batch.getQtyAvailable() - batch.getQtyReserved());
            }

            int missingQty = baseQtyNeeded - totalATP;
            if (missingQty > 0) {
                PurchaseRequestDetail prDetail = new PurchaseRequestDetail();
                prDetail.setPurchaseRequest(pr);
                prDetail.setProduct(product);
                prDetail.setRequestedQty(missingQty);
                prDetail.setUom(product.getBaseUoM());
                prDetail.setSalesOrderDetail(soDetail);
                pr.getDetails().add(prDetail);
            }
        }

        prDao.save(pr);
        for (PurchaseRequestDetail detail : pr.getDetails()) {
            prDetailDao.save(detail);
        }
        return prNumber;
    }

    // ==================== Delete ====================

    @Override
    public void deleteSO(Integer soId) {
        SalesOrder so = soDao.findById(soId)
                .orElseThrow(() -> new IllegalArgumentException("Sales Order not found: " + soId));
        if (!"Draft".equals(so.getSoStatus()) && !"Rejected".equals(so.getSoStatus())) {
            throw new IllegalArgumentException("Only Draft or Rejected SOs can be deleted.");
        }
        prDao.findByRelatedSalesOrderId(soId).ifPresent(pr -> {
            prDetailDao.deleteByPrId(pr.getPrId());
            prDao.deleteById(pr.getPrId());
        });
        soDetailDao.deleteBySoId(soId);
        soDao.deleteById(soId);
    }

    // ==================== Approve (with PR branching) ====================

    @Override
    public String approveSO(Integer soId, User currentUser) {
        SalesOrder so = soDao.findById(soId)
                .orElseThrow(() -> new IllegalArgumentException("Sales Order not found: " + soId));
        so.setDetails(soDetailDao.findBySoId(soId));

        // IDEMPOTENCY GUARD: If SO is already Approved/Completed, return immediately
        if ("Approved".equals(so.getSoStatus()) || "Completed".equals(so.getSoStatus())) {
            return null; // Already processed, safe to skip
        }

        // FIX: Use DB-level check instead of so.getGoodsIssueNotes() (Hibernate 1st-level cache).
        // Within the same transaction, the in-memory list may be stale — a GIN inserted
        // earlier in the loopback may not be reflected in so.getGoodsIssueNotes() yet.
        // existsBySalesOrder_SoId() issues an actual SELECT EXISTS, bypassing the cache.
        if (ginDao.existsBySalesOrderId(soId)) {
            return null; // GIN already exists in DB, skip to prevent duplicate
        }

        // Only allow approval from valid source states
        if (!"Pending Approval".equals(so.getSoStatus()) && !"Waiting for Stock".equals(so.getSoStatus())) {
            throw new IllegalArgumentException("SO cannot be approved in its current status: " + so.getSoStatus());
        }

        Warehouse warehouse = so.getWarehouse();

        // Check if SO has linked PR
        Optional<PurchaseRequest> linkedPR = prDao.findByRelatedSalesOrderId(soId);

        // BRANCH A: Has PR and PR is NOT yet Completed → Waiting for Stock
        if (linkedPR.isPresent()) {
            PurchaseRequest pr = linkedPR.get();
            if (!"Completed".equals(pr.getStatus())) {
                // PR exists and not completed → set PR to Approved (if still Pending), SO to Waiting
                if ("Pending".equals(pr.getStatus())) {
                    pr.setStatus("Approved");
                    prDao.save(pr);
                }
                so.setSoStatus("Waiting for Stock");
                soDao.save(so);
                return "SO approved. Waiting for incoming stock (PR " + pr.getPrNumber() + ").";
            }
            // If PR is Completed → fall through to Branch B (called from loopback)
        }

        // BRANCH B: No PR or PR already Completed → FIFO Reserve + auto-GIN
        List<GINAllocation> ginAllocations = new ArrayList<>();

        for (SalesOrderDetail soDetail : so.getDetails()) {
            Product product = soDetail.getProduct();
            BigDecimal conversionFactor = getConversionFactor(product, soDetail.getUom());
            int baseQtyNeeded = BigDecimal.valueOf(soDetail.getOrderedQty())
                    .multiply(conversionFactor).intValue();

            List<StockBatch> batches = stockBatchDao
                    .findByWarehouseIdAndProductIdOrderByArrivalDateTimeAsc(
                            warehouse.getWarehouseId(), product.getProductId());

            int remainingToReserve = baseQtyNeeded;

            for (StockBatch batch : batches) {
                if (remainingToReserve <= 0) break;
                int freeQty = batch.getQtyAvailable() - batch.getQtyReserved();
                if (freeQty <= 0) continue;

                int reserveQty = Math.min(freeQty, remainingToReserve);
                batch.setQtyReserved(batch.getQtyReserved() + reserveQty);
                stockBatchDao.save(batch);

                // Log StockMovement: Reserve / Reserved
                StockMovement movement = StockMovement.builder()
                        .warehouse(warehouse)
                        .product(product)
                        .bin(batch.getBin())
                        .batchNumber(batch.getBatchNumber())
                        .movementType("Reserve")
                        .stockType("Reserved")
                        .quantity(reserveQty)
                        .uom(product.getBaseUoM())
                        .balanceAfter(batch.getQtyReserved())
                        .build();
                stockMovementDao.save(movement);

                ginAllocations.add(new GINAllocation(soDetail, product, batch, reserveQty));
                remainingToReserve -= reserveQty;
            }

            if (remainingToReserve > 0) {
                throw new IllegalArgumentException(
                        "Insufficient available stock for product '" + product.getProductName()
                        + "'. Missing " + remainingToReserve + " " + product.getBaseUoM() + ".");
            }
        }

        // SO → Approved
        so.setSoStatus("Approved");
        soDao.save(so);

        // Auto-generate GIN (Draft)
        String ginNumber = generateGINNumber();
        GoodsIssueNote gin = new GoodsIssueNote();
        gin.setGinNumber(ginNumber);
        gin.setSalesOrder(so);
        gin.setWarehouse(warehouse);
        gin.setGiStatus("Draft");
        gin.setDetails(new ArrayList<>());

        for (GINAllocation alloc : ginAllocations) {
            GoodsIssueDetail ginDetail = new GoodsIssueDetail();
            ginDetail.setGoodsIssueNote(gin);
            ginDetail.setSalesOrderDetail(alloc.soDetail);
            ginDetail.setProduct(alloc.product);
            ginDetail.setIssuedQty(0);
            ginDetail.setPlannedQty(alloc.reservedQty); // FIX: batch-level reserved qty, not 0
            ginDetail.setUom(alloc.soDetail.getUom());
            ginDetail.setBatchNumber(alloc.batch.getBatchNumber());
            ginDetail.setBin(alloc.batch.getBin());
            gin.getDetails().add(ginDetail);
        }

        ginDao.save(gin);
        for (GoodsIssueDetail detail : gin.getDetails()) {
            ginDetailDao.save(detail);
        }
        return ginNumber;
    }

    // ==================== Reject (with PR cascade) ====================

    @Override
    public void rejectSO(Integer soId, String reason) {
        SalesOrder so = soDao.findById(soId)
                .orElseThrow(() -> new IllegalArgumentException("Sales Order not found: " + soId));

        if (!"Pending Approval".equals(so.getSoStatus())) {
            throw new IllegalArgumentException("Only SOs with 'Pending Approval' status can be rejected.");
        }

        so.setSoStatus("Rejected");
        so.setRejectReason(reason);
        soDao.save(so);

        // Cascade: reject any linked PRs that are still active
        List<PurchaseRequest> linkedPRs = prDao.findByRelatedSalesOrderIdAndStatusIn(
                soId, List.of("Pending", "Approved"));
        for (PurchaseRequest pr : linkedPRs) {
            pr.setStatus("Rejected");
            prDao.save(pr);
        }
    }

    // ==================== Number Generation ====================

    private String generateSONumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "SO-" + dateStr + "-";
        String maxNumber = soDao.findMaxSoNumber(prefix);
        int nextNum = 1;
        if (maxNumber != null) {
            String suffix = maxNumber.substring(prefix.length());
            nextNum = Integer.parseInt(suffix) + 1;
        }
        return prefix + String.format("%03d", nextNum);
    }

    private String generateGINNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "GIN-" + dateStr + "-";
        String maxNumber = ginDao.findMaxGinNumber(prefix);
        int nextNum = 1;
        if (maxNumber != null) {
            String suffix = maxNumber.substring(prefix.length());
            nextNum = Integer.parseInt(suffix) + 1;
        }
        return prefix + String.format("%03d", nextNum);
    }

    private String generatePRNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "PR-" + dateStr + "-";
        String maxNumber = prDao.findMaxPrNumber(prefix);
        int nextNum = 1;
        if (maxNumber != null) {
            String suffix = maxNumber.substring(prefix.length());
            nextNum = Integer.parseInt(suffix) + 1;
        }
        return prefix + String.format("%03d", nextNum);
    }

    // ==================== UoM Conversion ====================

    private BigDecimal getConversionFactor(Product product, String uom) {
        if (uom.equals(product.getBaseUoM())) return BigDecimal.ONE;
        List<ProductUoMConversion> conversions = uomConversionDao.findByProductId(product.getProductId());
        for (ProductUoMConversion conv : conversions) {
            if (conv.getFromUoM().equals(uom)) return BigDecimal.valueOf(conv.getConversionFactor());
        }
        return BigDecimal.ONE;
    }

    // ==================== Mapping: Entity -> DTO ====================

    private SaleOrderDTO mapToDTO(SalesOrder so) {
        Optional<PurchaseRequest> linkedPR = prDao.findByRelatedSalesOrderId(so.getSoId());
        SaleOrderDTO dto = SaleOrderDTO.builder()
                .soId(so.getSoId())
                .soNumber(so.getSoNumber())
                .customerId(so.getCustomer() != null ? so.getCustomer().getPartnerId() : null)
                .customerName(so.getCustomer() != null ? so.getCustomer().getPartnerName() : null)
                .warehouseId(so.getWarehouse() != null ? so.getWarehouse().getWarehouseId() : null)
                .warehouseName(so.getWarehouse() != null ? so.getWarehouse().getWarehouseName() : null)
                .soStatus(so.getSoStatus())
                .rejectReason(so.getRejectReason())
                .hasPR(linkedPR.isPresent())
                .prNumber(linkedPR.map(PurchaseRequest::getPrNumber).orElse(null))
                .prStatus(linkedPR.map(PurchaseRequest::getStatus).orElse(null))
                .build();

        if (so.getDetails() != null) {
            List<SaleOrderDetailDTO> detailDTOs = so.getDetails().stream()
                    .map(this::mapDetailToDTO).collect(Collectors.toList());
            dto.setDetails(detailDTOs);
        }
        return dto;
    }

    private SaleOrderDetailDTO mapDetailToDTO(SalesOrderDetail detail) {
        String displayName = detail.getProduct() != null
                ? detail.getProduct().getSku() + " - " + detail.getProduct().getProductName() : "";
        return SaleOrderDetailDTO.builder()
                .soDetailId(detail.getSoDetailId())
                .productId(detail.getProduct() != null ? detail.getProduct().getProductId() : null)
                .productDisplayName(displayName)
                .uom(detail.getUom())
                .orderedQty(detail.getOrderedQty())
                .build();
    }

    // ==================== Mapping: DTO -> Entity ====================

    private SalesOrder buildSalesOrder(SaleOrderDTO dto, User currentUser) {
        SalesOrder so;
        if (dto.getSoId() != null) {
            so = soDao.findById(dto.getSoId())
                    .orElseThrow(() -> new IllegalArgumentException("Sales Order not found: " + dto.getSoId()));
            if (!"Draft".equals(so.getSoStatus()) && !"Rejected".equals(so.getSoStatus())) {
                throw new IllegalArgumentException("Only Draft or Rejected SOs can be edited.");
            }
            prDao.findByRelatedSalesOrderId(so.getSoId()).ifPresent(pr -> {
                prDetailDao.deleteByPrId(pr.getPrId());
                prDao.deleteById(pr.getPrId());
            });
        } else {
            so = new SalesOrder();
            so.setSoNumber(generateSONumber());
            Warehouse warehouse = new Warehouse();
            warehouse.setWarehouseId(currentUser.getWarehouse().getWarehouseId());
            so.setWarehouse(warehouse);
        }

        if (dto.getCustomerId() != null) {
            Partner customer = partnerDao.findById(dto.getCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + dto.getCustomerId()));
            so.setCustomer(customer);
        }
        return so;
    }

    private List<SaleOrderDetailDTO> filterEmptyDetails(List<SaleOrderDetailDTO> details) {
        if (details == null) return new ArrayList<>();
        return details.stream().filter(d -> d.getProductId() != null).collect(Collectors.toList());
    }

    private record GINAllocation(SalesOrderDetail soDetail, Product product, StockBatch batch, int reservedQty) {}

    private List<SalesOrderDetail> buildDetails(SaleOrderDTO dto) {
        List<SaleOrderDetailDTO> validDetails = filterEmptyDetails(dto.getDetails());
        List<SalesOrderDetail> details = new ArrayList<>();
        for (SaleOrderDetailDTO detailDTO : validDetails) {
            SalesOrderDetail detail = new SalesOrderDetail();
            Product product = productDao.findById(detailDTO.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + detailDTO.getProductId()));
            detail.setProduct(product);
            detail.setUom(detailDTO.getUom());
            detail.setOrderedQty(detailDTO.getOrderedQty());
            detail.setIssuedQty(0);
            details.add(detail);
        }
        return details;
    }

    private SalesOrder saveSalesOrder(SalesOrder so, List<SalesOrderDetail> details, boolean clearExisting) {
        SalesOrder saved = soDao.save(so);
        if (clearExisting) {
            soDetailDao.deleteBySoId(saved.getSoId());
        }
        for (SalesOrderDetail detail : details) {
            detail.setSalesOrder(saved);
            soDetailDao.save(detail);
        }
        saved.setDetails(details);
        return saved;
    }
}
