package com.minhhai.wms.service.impl;

import com.minhhai.wms.dto.SaleOrderDTO;
import com.minhhai.wms.dto.SaleOrderDetailDTO;
import com.minhhai.wms.dto.StockCheckResult;
import com.minhhai.wms.entity.*;
import com.minhhai.wms.repository.*;
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

    private final SalesOrderRepository soRepository;
    private final PartnerRepository partnerRepository;
    private final ProductRepository productRepository;
    private final ProductUoMConversionRepository uomConversionRepository;
    private final GoodsIssueNoteRepository ginRepository;
    private final StockBatchRepository stockBatchRepository;
    private final StockMovementRepository stockMovementRepository;
    private final PurchaseRequestRepository prRepository;
    private final PurchaseRequestDetailRepository prDetailRepository;

    // ==================== Query Methods ====================

    @Override
    @Transactional(readOnly = true)
    public List<SaleOrderDTO> getSOsByWarehouse(Integer warehouseId, String status, Integer customerId) {
        List<SalesOrder> orders;
        boolean hasStatus = status != null && !status.isBlank();
        boolean hasCustomer = customerId != null;

        if (hasStatus && hasCustomer) {
            orders = soRepository.findByWarehouse_WarehouseIdAndSoStatusAndCustomer_PartnerId(warehouseId, status, customerId);
        } else if (hasStatus) {
            orders = soRepository.findByWarehouse_WarehouseIdAndSoStatus(warehouseId, status);
        } else if (hasCustomer) {
            orders = soRepository.findByWarehouse_WarehouseIdAndCustomer_PartnerId(warehouseId, customerId);
        } else {
            orders = soRepository.findByWarehouse_WarehouseId(warehouseId);
        }
        return orders.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SaleOrderDTO getSOById(Integer soId) {
        SalesOrder so = soRepository.findById(soId)
                .orElseThrow(() -> new IllegalArgumentException("Sales Order not found: " + soId));
        return mapToDTO(so);
    }

    // ==================== Save Draft ====================

    @Override
    public SaleOrderDTO saveDraft(SaleOrderDTO dto, User currentUser) {
        SalesOrder so = mapToEntity(dto, currentUser);
        so.setSoStatus("Draft");
        so.setRejectReason(null);
        so = soRepository.save(so);
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
        SalesOrder so = mapToEntity(dto, currentUser);
        so.setSoStatus("Draft");           // ← Keep Draft until decision
        so.setRejectReason(null);
        so = soRepository.save(so);

        Integer warehouseId = so.getWarehouse().getWarehouseId();

        // 3. ATP check for each product line
        List<StockCheckResult.ShortageItem> shortages = new ArrayList<>();
        for (SalesOrderDetail soDetail : so.getDetails()) {
            Product product = soDetail.getProduct();
            BigDecimal conversionFactor = getConversionFactor(product, soDetail.getUom());
            int baseQtyNeeded = BigDecimal.valueOf(soDetail.getOrderedQty())
                    .multiply(conversionFactor).intValue();

            // Total_ATP = Σ(qtyAvailable - qtyReserved) for this product in THIS warehouse
            List<StockBatch> batches = stockBatchRepository
                    .findByWarehouseWarehouseIdAndProductProductId(warehouseId, product.getProductId());
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
                so = soRepository.save(so);
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
        so = soRepository.save(so);
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
            List<StockBatch> batches = stockBatchRepository
                    .findByWarehouseWarehouseIdAndProductProductId(warehouseId, product.getProductId());
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

        prRepository.save(pr);
        return prNumber;
    }

    // ==================== Delete ====================

    @Override
    public void deleteSO(Integer soId) {
        SalesOrder so = soRepository.findById(soId)
                .orElseThrow(() -> new IllegalArgumentException("Sales Order not found: " + soId));
        if (!"Draft".equals(so.getSoStatus()) && !"Rejected".equals(so.getSoStatus())) {
            throw new IllegalArgumentException("Only Draft or Rejected SOs can be deleted.");
        }
        soRepository.delete(so);
    }

    // ==================== Approve (with PR branching) ====================

    @Override
    public String approveSO(Integer soId, User currentUser) {
        SalesOrder so = soRepository.findById(soId)
                .orElseThrow(() -> new IllegalArgumentException("Sales Order not found: " + soId));

        // IDEMPOTENCY GUARD: If SO is already Approved/Completed, return immediately
        if ("Approved".equals(so.getSoStatus()) || "Completed".equals(so.getSoStatus())) {
            return null; // Already processed, safe to skip
        }

        // Also guard: if a GIN already exists for this SO, don't create another one
        if (so.getGoodsIssueNotes() != null && !so.getGoodsIssueNotes().isEmpty()) {
            return null; // GIN already exists, skip
        }

        // Only allow approval from valid source states
        if (!"Pending Approval".equals(so.getSoStatus()) && !"Waiting for Stock".equals(so.getSoStatus())) {
            throw new IllegalArgumentException("SO cannot be approved in its current status: " + so.getSoStatus());
        }

        Warehouse warehouse = so.getWarehouse();

        // Check if SO has linked PR
        Optional<PurchaseRequest> linkedPR = prRepository.findByRelatedSalesOrder_SoId(soId);

        // BRANCH A: Has PR and PR is NOT yet Completed → Waiting for Stock
        if (linkedPR.isPresent()) {
            PurchaseRequest pr = linkedPR.get();
            if (!"Completed".equals(pr.getStatus())) {
                // PR exists and not completed → set PR to Approved (if still Pending), SO to Waiting
                if ("Pending".equals(pr.getStatus())) {
                    pr.setStatus("Approved");
                    prRepository.save(pr);
                }
                so.setSoStatus("Waiting for Stock");
                soRepository.save(so);
                return "SO đã được duyệt. Đang chờ hàng về kho (PR " + pr.getPrNumber() + ").";
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

            List<StockBatch> batches = stockBatchRepository
                    .findByWarehouse_WarehouseIdAndProduct_ProductIdOrderByArrivalDateTimeAsc(
                            warehouse.getWarehouseId(), product.getProductId());

            int remainingToReserve = baseQtyNeeded;

            for (StockBatch batch : batches) {
                if (remainingToReserve <= 0) break;
                int freeQty = batch.getQtyAvailable() - batch.getQtyReserved();
                if (freeQty <= 0) continue;

                int reserveQty = Math.min(freeQty, remainingToReserve);
                batch.setQtyReserved(batch.getQtyReserved() + reserveQty);
                stockBatchRepository.save(batch);

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
                stockMovementRepository.save(movement);

                ginAllocations.add(new GINAllocation(soDetail, product, batch, reserveQty));
                remainingToReserve -= reserveQty;
            }

            if (remainingToReserve > 0) {
                throw new IllegalArgumentException(
                        "Không đủ tồn kho cho sản phẩm '" + product.getProductName()
                        + "'. Thiếu " + remainingToReserve + " " + product.getBaseUoM() + ".");
            }
        }

        // SO → Approved
        so.setSoStatus("Approved");
        soRepository.save(so);

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
            ginDetail.setUom(alloc.soDetail.getUom());
            ginDetail.setBatchNumber(alloc.batch.getBatchNumber());
            ginDetail.setBin(alloc.batch.getBin());
            gin.getDetails().add(ginDetail);
        }

        ginRepository.save(gin);
        return ginNumber;
    }

    // ==================== Reject (with PR cascade) ====================

    @Override
    public void rejectSO(Integer soId, String reason) {
        SalesOrder so = soRepository.findById(soId)
                .orElseThrow(() -> new IllegalArgumentException("Sales Order not found: " + soId));

        if (!"Pending Approval".equals(so.getSoStatus())) {
            throw new IllegalArgumentException("Only SOs with 'Pending Approval' status can be rejected.");
        }

        so.setSoStatus("Rejected");
        so.setRejectReason(reason);
        soRepository.save(so);

        // Cascade: reject any linked PRs that are still active
        List<PurchaseRequest> linkedPRs = prRepository.findByRelatedSalesOrder_SoIdAndStatusIn(
                soId, List.of("Pending", "Approved"));
        for (PurchaseRequest pr : linkedPRs) {
            pr.setStatus("Rejected");
            prRepository.save(pr);
        }
    }

    // ==================== Number Generation ====================

    private String generateSONumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "SO-" + dateStr + "-";
        String maxNumber = soRepository.findMaxSoNumber(prefix);
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
        String maxNumber = ginRepository.findMaxGinNumber(prefix);
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
        String maxNumber = prRepository.findMaxPrNumber(prefix);
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
        List<ProductUoMConversion> conversions = uomConversionRepository.findByProduct_ProductId(product.getProductId());
        for (ProductUoMConversion conv : conversions) {
            if (conv.getFromUoM().equals(uom)) return BigDecimal.valueOf(conv.getConversionFactor());
        }
        return BigDecimal.ONE;
    }

    // ==================== Mapping: Entity -> DTO ====================

    private SaleOrderDTO mapToDTO(SalesOrder so) {
        // Check for linked PR
        Optional<PurchaseRequest> linkedPR = prRepository.findByRelatedSalesOrder_SoId(so.getSoId());
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

    private SalesOrder mapToEntity(SaleOrderDTO dto, User currentUser) {
        SalesOrder so;
        if (dto.getSoId() != null) {
            so = soRepository.findById(dto.getSoId())
                    .orElseThrow(() -> new IllegalArgumentException("Sales Order not found: " + dto.getSoId()));
            if (!"Draft".equals(so.getSoStatus()) && !"Rejected".equals(so.getSoStatus())) {
                throw new IllegalArgumentException("Only Draft or Rejected SOs can be edited.");
            }
            if (so.getDetails() != null) so.getDetails().clear();
        } else {
            so = new SalesOrder();
            so.setSoNumber(generateSONumber());
            so.setWarehouse(currentUser.getWarehouse());
            so.setDetails(new ArrayList<>());
        }

        if (dto.getCustomerId() != null) {
            Partner customer = partnerRepository.findById(dto.getCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + dto.getCustomerId()));
            so.setCustomer(customer);
        }

        List<SaleOrderDetailDTO> validDetails = filterEmptyDetails(dto.getDetails());
        for (SaleOrderDetailDTO detailDTO : validDetails) {
            SalesOrderDetail detail = new SalesOrderDetail();
            detail.setSalesOrder(so);
            Product product = productRepository.findById(detailDTO.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + detailDTO.getProductId()));
            detail.setProduct(product);
            detail.setUom(detailDTO.getUom());
            detail.setOrderedQty(detailDTO.getOrderedQty());
            detail.setIssuedQty(0);
            so.getDetails().add(detail);
        }
        return so;
    }

    private List<SaleOrderDetailDTO> filterEmptyDetails(List<SaleOrderDetailDTO> details) {
        if (details == null) return new ArrayList<>();
        return details.stream().filter(d -> d.getProductId() != null).collect(Collectors.toList());
    }

    private record GINAllocation(SalesOrderDetail soDetail, Product product, StockBatch batch, int reservedQty) {}
}
