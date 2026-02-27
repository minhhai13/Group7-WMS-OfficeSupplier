package com.minhhai.wms.service.impl;

import com.minhhai.wms.dto.SaleOrderDTO;
import com.minhhai.wms.dto.SaleOrderDetailDTO;
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

    // ==================== Save / Submit ====================

    @Override
    public SaleOrderDTO saveDraft(SaleOrderDTO dto, User currentUser) {
        SalesOrder so = mapToEntity(dto, currentUser);
        so.setSoStatus("Draft");
        so.setRejectReason(null);
        so = soRepository.save(so);
        return mapToDTO(so);
    }

    @Override
    public SaleOrderDTO submitForApproval(SaleOrderDTO dto, User currentUser) {
        // Validate: at least 1 non-empty detail line
        List<SaleOrderDetailDTO> allDetails = dto.getDetails();
        if (allDetails == null || allDetails.isEmpty()) {
            throw new IllegalArgumentException("At least one product line is required to submit.");
        }

        for (int i = 0; i < allDetails.size(); i++) {
            SaleOrderDetailDTO d = allDetails.get(i);
            if (d.getProductId() == null) {
                throw new IllegalArgumentException("Product is required on line " + (i + 1) + ".");
            }
            if (d.getUom() == null || d.getUom().isBlank()) {
                throw new IllegalArgumentException("UoM is required on line " + (i + 1) + ".");
            }
            if (d.getOrderedQty() == null || d.getOrderedQty() <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than 0 on line " + (i + 1) + ".");
            }
        }

        SalesOrder so = mapToEntity(dto, currentUser);
        so.setSoStatus("Pending Approval");
        so.setRejectReason(null);
        so = soRepository.save(so);
        return mapToDTO(so);
    }

    @Override
    public void deleteSO(Integer soId) {
        SalesOrder so = soRepository.findById(soId)
                .orElseThrow(() -> new IllegalArgumentException("Sales Order not found: " + soId));
        if (!"Draft".equals(so.getSoStatus()) && !"Rejected".equals(so.getSoStatus())) {
            throw new IllegalArgumentException("Only Draft or Rejected SOs can be deleted.");
        }
        soRepository.delete(so);
    }

    // ==================== Approve / Reject (Phase 2) ====================

    @Override
    public String approveSO(Integer soId, User currentUser) {
        SalesOrder so = soRepository.findById(soId)
                .orElseThrow(() -> new IllegalArgumentException("Sales Order not found: " + soId));

        if (!"Pending Approval".equals(so.getSoStatus())) {
            throw new IllegalArgumentException("Only SOs with 'Pending Approval' status can be approved.");
        }

        Warehouse warehouse = so.getWarehouse();

        // 1. FIFO Reservation: for each SO detail, reserve stock from batches
        // We also track which batches were reserved for GIN detail creation
        List<GINAllocation> ginAllocations = new ArrayList<>();

        for (SalesOrderDetail soDetail : so.getDetails()) {
            Product product = soDetail.getProduct();
            BigDecimal conversionFactor = getConversionFactor(product, soDetail.getUom());
            int baseQtyNeeded = BigDecimal.valueOf(soDetail.getOrderedQty())
                    .multiply(conversionFactor).intValue();

            // FIFO: find batches ordered by arrival time
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

                // Track for GIN detail creation
                ginAllocations.add(new GINAllocation(soDetail, product, batch, reserveQty));

                remainingToReserve -= reserveQty;
            }

            if (remainingToReserve > 0) {
                throw new IllegalArgumentException(
                        "Không đủ tồn kho cho sản phẩm '" + product.getProductName()
                        + "'. Thiếu " + remainingToReserve + " " + product.getBaseUoM() + ".");
            }
        }

        // 2. SO status → Approved
        so.setSoStatus("Approved");
        soRepository.save(so);

        // 3. Auto-generate GIN (Draft)
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
            ginDetail.setIssuedQty(0); // Storekeeper fills in Phase 3
            ginDetail.setUom(alloc.soDetail.getUom());
            ginDetail.setBatchNumber(alloc.batch.getBatchNumber());
            ginDetail.setBin(alloc.batch.getBin());
            gin.getDetails().add(ginDetail);
        }

        ginRepository.save(gin);

        return ginNumber;
    }

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
    }

    // ==================== UoM Lookup ====================

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, String>> getAvailableUoMs(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        List<Map<String, String>> uomList = new ArrayList<>();

        // 1. Base UoM
        Map<String, String> baseEntry = new LinkedHashMap<>();
        baseEntry.put("uom", product.getBaseUoM());
        baseEntry.put("label", product.getBaseUoM() + " (Base)");
        uomList.add(baseEntry);

        // 2. All FromUoMs from conversions
        List<ProductUoMConversion> conversions = uomConversionRepository.findByProduct_ProductId(productId);
        for (ProductUoMConversion conv : conversions) {
            if (!conv.getFromUoM().equals(product.getBaseUoM())) {
                Map<String, String> entry = new LinkedHashMap<>();
                entry.put("uom", conv.getFromUoM());
                entry.put("label", conv.getFromUoM() + " (1 " + conv.getFromUoM() + " = " + conv.getConversionFactor() + " " + conv.getToUoM() + ")");
                uomList.add(entry);
            }
        }

        return uomList;
    }

    // ==================== SO Number Generation ====================

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

    // ==================== UoM Conversion ====================

    private BigDecimal getConversionFactor(Product product, String uom) {
        if (uom.equals(product.getBaseUoM())) {
            return BigDecimal.ONE;
        }
        List<ProductUoMConversion> conversions = uomConversionRepository.findByProduct_ProductId(product.getProductId());
        for (ProductUoMConversion conv : conversions) {
            if (conv.getFromUoM().equals(uom)) {
                return BigDecimal.valueOf(conv.getConversionFactor());
            }
        }
        return BigDecimal.ONE;
    }

    // ==================== Mapping: Entity -> DTO ====================

    private SaleOrderDTO mapToDTO(SalesOrder so) {
        SaleOrderDTO dto = SaleOrderDTO.builder()
                .soId(so.getSoId())
                .soNumber(so.getSoNumber())
                .customerId(so.getCustomer() != null ? so.getCustomer().getPartnerId() : null)
                .customerName(so.getCustomer() != null ? so.getCustomer().getPartnerName() : null)
                .warehouseId(so.getWarehouse() != null ? so.getWarehouse().getWarehouseId() : null)
                .warehouseName(so.getWarehouse() != null ? so.getWarehouse().getWarehouseName() : null)
                .soStatus(so.getSoStatus())
                .rejectReason(so.getRejectReason())
                .build();

        if (so.getDetails() != null) {
            List<SaleOrderDetailDTO> detailDTOs = so.getDetails().stream()
                    .map(this::mapDetailToDTO)
                    .collect(Collectors.toList());
            dto.setDetails(detailDTOs);
        }

        return dto;
    }

    private SaleOrderDetailDTO mapDetailToDTO(SalesOrderDetail detail) {
        String displayName = "";
        if (detail.getProduct() != null) {
            displayName = detail.getProduct().getSku() + " - " + detail.getProduct().getProductName();
        }
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
            // Update existing SO
            so = soRepository.findById(dto.getSoId())
                    .orElseThrow(() -> new IllegalArgumentException("Sales Order not found: " + dto.getSoId()));
            // Only allow editing Draft or Rejected SOs
            if (!"Draft".equals(so.getSoStatus()) && !"Rejected".equals(so.getSoStatus())) {
                throw new IllegalArgumentException("Only Draft or Rejected SOs can be edited.");
            }
            // Clear old details (orphanRemoval will delete them)
            if (so.getDetails() != null) {
                so.getDetails().clear();
            }
        } else {
            // Create new SO
            so = new SalesOrder();
            so.setSoNumber(generateSONumber());
            so.setWarehouse(currentUser.getWarehouse());
            so.setDetails(new ArrayList<>());
        }

        // Set customer
        if (dto.getCustomerId() != null) {
            Partner customer = partnerRepository.findById(dto.getCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + dto.getCustomerId()));
            so.setCustomer(customer);
        }

        // Map detail lines (filter empty ones)
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

    // ==================== Helpers ====================

    private List<SaleOrderDetailDTO> filterEmptyDetails(List<SaleOrderDetailDTO> details) {
        if (details == null) return new ArrayList<>();
        return details.stream()
                .filter(d -> d.getProductId() != null)
                .collect(Collectors.toList());
    }

    /**
     * Internal record to track batch allocations for GIN detail creation during approval.
     */
    private record GINAllocation(SalesOrderDetail soDetail, Product product, StockBatch batch, int reservedQty) {}
}
