package com.minhhai.wms.service.impl;

import com.minhhai.wms.dto.PurchaseOrderDTO;
import com.minhhai.wms.dto.PurchaseOrderDetailDTO;
import com.minhhai.wms.entity.*;
import com.minhhai.wms.repository.*;
import com.minhhai.wms.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository poRepository;
    private final PartnerRepository partnerRepository;
    private final ProductRepository productRepository;
    private final ProductUoMConversionRepository uomConversionRepository;
    private final GoodsReceiptNoteRepository grnRepository;
    private final GoodsReceiptDetailRepository grnDetailRepository;
    private final BinRepository binRepository;
    private final StockBatchRepository stockBatchRepository;

    // ==================== Query Methods ====================

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseOrderDTO> getPOsByWarehouse(Integer warehouseId, String status, Integer supplierId, Pageable pageable) {
        Page<PurchaseOrder> orders;

        boolean hasStatus = status != null && !status.isBlank();
        boolean hasSupplier = supplierId != null;

        if (hasStatus && hasSupplier) {
            orders = poRepository.findByWarehouse_WarehouseIdAndPoStatusAndSupplier_PartnerId(warehouseId, status, supplierId, pageable);
        } else if (hasStatus) {
            orders = poRepository.findByWarehouse_WarehouseIdAndPoStatus(warehouseId, status, pageable);
        } else if (hasSupplier) {
            orders = poRepository.findByWarehouse_WarehouseIdAndSupplier_PartnerId(warehouseId, supplierId, pageable);
        } else {
            orders = poRepository.findByWarehouse_WarehouseId(warehouseId, pageable);
        }

        return orders.map(this::mapToDTO); // Page hỗ trợ sẵn hàm map()
    }
    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderDTO getPOById(Integer poId) {
        PurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase Order not found: " + poId));
        return mapToDTO(po);
    }

    // ==================== Save / Submit ====================

    @Override
    public PurchaseOrderDTO saveDraft(PurchaseOrderDTO dto, User currentUser) {
        PurchaseOrder po = mapToEntity(dto, currentUser);
        po.setPoStatus("Draft");
        po.setRejectReason(null);
        po = poRepository.save(po);
        return mapToDTO(po);
    }

    @Override
    public PurchaseOrderDTO submitForApproval(PurchaseOrderDTO dto, User currentUser) {
        // Validate: at least 1 non-empty detail line
        List<PurchaseOrderDetailDTO> allDetails = dto.getDetails();
        if (allDetails == null || allDetails.isEmpty()) {
            throw new IllegalArgumentException("At least one product line is required to submit.");
        }

        for (int i = 0; i < allDetails.size(); i++) {
            PurchaseOrderDetailDTO d = allDetails.get(i);
            // Kiểm tra xem dòng này có dữ liệu không.
            // Nếu người dùng bấm "Add line" mà không nhập gì, productId sẽ null.
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

        PurchaseOrder po = mapToEntity(dto, currentUser);
        po.setPoStatus("Pending Approval");
        po.setRejectReason(null);
        po = poRepository.save(po);
        return mapToDTO(po);
    }

    @Override
    public void deletePO(Integer poId) {
        PurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase Order not found: " + poId));
        if (!"Draft".equals(po.getPoStatus()) && !"Rejected".equals(po.getPoStatus())) {
            throw new IllegalArgumentException("Only Draft or Rejected POs can be deleted.");
        }
        poRepository.delete(po);
    }

    // ==================== Approve / Reject ====================

    @Override
    public String approvePO(Integer poId, User currentUser) {
        PurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase Order not found: " + poId));

        if (!"Pending Approval".equals(po.getPoStatus())) {
            throw new IllegalArgumentException("Only POs with 'Pending Approval' status can be approved.");
        }

        // 1. Change PO status
        po.setPoStatus("Approved");
        po.setRejectReason(null);

        // 2. Generate GRN
        String grnNumber = generateGRNNumber();
        GoodsReceiptNote grn = new GoodsReceiptNote();
        grn.setGrnNumber(grnNumber);
        grn.setPurchaseOrder(po);
        grn.setWarehouse(po.getWarehouse());
        grn.setGrStatus("Draft");
        grn.setDetails(new ArrayList<>());

        // 3. For each PO detail → create GRN detail
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (PurchaseOrderDetail poDetail : po.getDetails()) {
            Product product = poDetail.getProduct();

            // BatchNumber unique per PO+date+product: BATCH-yyyyMMdd-PO{poNumber}-P{productId}
            String batchNumber = "BATCH-" + dateStr + "-" + po.getPoNumber() + "-P" + product.getProductId();

            // Calculate incoming weight with UoM conversion
            BigDecimal conversionFactor = getConversionFactor(product, poDetail.getUom());
            // incomingWeight = orderedQty * conversionFactor * unitWeight
            BigDecimal incomingWeight = BigDecimal.valueOf(poDetail.getOrderedQty())
                    .multiply(conversionFactor)
                    .multiply(product.getUnitWeight());

            // System-allocated Bin by weight capacity (including virtual/Draft GRN capacity)
            Bin allocatedBin = allocateBin(po.getWarehouse().getWarehouseId(), incomingWeight, product.getProductName());

            GoodsReceiptDetail grnDetail = new GoodsReceiptDetail();
            grnDetail.setGoodsReceiptNote(grn);
            grnDetail.setPurchaseOrderDetail(poDetail);
            grnDetail.setProduct(product);
            grnDetail.setReceivedQty(0); // Storekeeper updates in Phase 3
            grnDetail.setUom(poDetail.getUom());
            grnDetail.setBatchNumber(batchNumber);
            grnDetail.setBin(allocatedBin);

            grn.getDetails().add(grnDetail);
        }

        // 4. Save GRN (cascade saves details)
        // NO StockBatch/StockMovement updates — that's Phase 3 (Storekeeper Post)
        grnRepository.save(grn);
        poRepository.save(po);

        return grnNumber;
    }

    @Override
    public void rejectPO(Integer poId, String reason) {
        PurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> new IllegalArgumentException("Purchase Order not found: " + poId));

        if (!"Pending Approval".equals(po.getPoStatus())) {
            throw new IllegalArgumentException("Only POs with 'Pending Approval' status can be rejected.");
        }

        po.setPoStatus("Rejected");
        po.setRejectReason(reason);
        poRepository.save(po);
    }

    // ==================== PO Number Generation ====================

    private String generatePONumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "PO-" + dateStr + "-";
        String maxNumber = poRepository.findMaxPoNumber(prefix);

        int nextNum = 1;
        if (maxNumber != null) {
            // Lấy 3 số cuối của mã lớn nhất và cộng thêm 1
            String suffix = maxNumber.substring(prefix.length());
            nextNum = Integer.parseInt(suffix) + 1;
        }

        return prefix + String.format("%03d", nextNum);
    }

    private String generateGRNNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "GRN-" + dateStr + "-";
        String maxNumber = grnRepository.findMaxGrnNumber(prefix);

        int nextNum = 1;
        if (maxNumber != null) {
            String suffix = maxNumber.substring(prefix.length());
            nextNum = Integer.parseInt(suffix) + 1;
        }

        return prefix + String.format("%03d", nextNum);
    }

    /**
     * Get UoM conversion factor: how many base units in 1 of the given UoM.
     * If uom == baseUoM, returns 1.
     * Otherwise looks up ProductUoMConversion where fromUoM = uom.
     */
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
        // Fallback: no conversion found, treat as 1
        return BigDecimal.ONE;
    }

    /**
     * System-allocated Bin: find an active bin in the warehouse with enough remaining weight capacity.
     * Capacity check includes:
     *   1. Actual stock weight from StockBatch
     *   2. Virtual weight from Draft GRN allocations (not yet posted)
     */
    private Bin allocateBin(Integer warehouseId, BigDecimal incomingWeight, String productName) {
        List<Bin> activeBins = binRepository.findByWarehouseWarehouseIdAndIsActive(warehouseId, true);

        for (Bin bin : activeBins) {
            BigDecimal currentWeight = calculateCurrentBinWeight(bin);
            BigDecimal remainingCapacity = bin.getMaxWeight().subtract(currentWeight);

            if (remainingCapacity.compareTo(incomingWeight) >= 0) {
                return bin;
            }
        }

        throw new IllegalArgumentException(
                "Kho không đủ sức chứa cho mặt hàng '" + productName +
                "' (Yêu cầu: " + incomingWeight + " kg). Vui lòng giải phóng không gian hoặc thêm bin mới.");
    }

    /**
     * Calculate current bin weight = actual stock weight + virtual Draft GRN weight.
     */
    private BigDecimal calculateCurrentBinWeight(Bin bin) {
        BigDecimal totalWeight = BigDecimal.ZERO;

        // 1. Actual stock weight from StockBatch
        List<StockBatch> batches = stockBatchRepository.findByBinBinId(bin.getBinId());
        for (StockBatch batch : batches) {
            Product product = batch.getProduct();
            BigDecimal batchWeight = product.getUnitWeight()
                    .multiply(BigDecimal.valueOf(batch.getQtyAvailable()));
            totalWeight = totalWeight.add(batchWeight);
        }

        // 2. Virtual weight from Draft GRN allocations (not yet posted)
        List<GoodsReceiptDetail> draftDetails = grnDetailRepository.findDraftGrnDetailsByBinId(bin.getBinId());
        for (GoodsReceiptDetail grnDetail : draftDetails) {
            Product product = grnDetail.getProduct();
            // Use PO's ordered qty for weight calculation (receivedQty is 0 in Draft)
            PurchaseOrderDetail poDetail = grnDetail.getPurchaseOrderDetail();
            BigDecimal conversionFactor = getConversionFactor(product, grnDetail.getUom());
            BigDecimal virtualWeight = BigDecimal.valueOf(poDetail.getOrderedQty() - poDetail.getReceivedQty())
                    .multiply(conversionFactor)
                    .multiply(product.getUnitWeight());
            totalWeight = totalWeight.add(virtualWeight);
        }

        return totalWeight;
    }

    // ==================== Mapping: Entity -> DTO ====================

    private PurchaseOrderDTO mapToDTO(PurchaseOrder po) {
        PurchaseOrderDTO dto = PurchaseOrderDTO.builder()
                .poId(po.getPoId())
                .poNumber(po.getPoNumber())
                .supplierId(po.getSupplier() != null ? po.getSupplier().getPartnerId() : null)
                .supplierName(po.getSupplier() != null ? po.getSupplier().getPartnerName() : null)
                .warehouseId(po.getWarehouse() != null ? po.getWarehouse().getWarehouseId() : null)
                .warehouseName(po.getWarehouse() != null ? po.getWarehouse().getWarehouseName() : null)
                .poStatus(po.getPoStatus())
                .rejectReason(po.getRejectReason())
                .build();

        // Populate PR source info (which PRs generated this PO)
        if (po.getPurchaseRequests() != null && !po.getPurchaseRequests().isEmpty()) {
            String prNumbers = po.getPurchaseRequests().stream()
                    .map(PurchaseRequest::getPrNumber)
                    .collect(Collectors.joining(", "));
            dto.setSourcePRNumbers(prNumbers);
        }

        if (po.getDetails() != null) {
            List<PurchaseOrderDetailDTO> detailDTOs = po.getDetails().stream()
                    .map(this::mapDetailToDTO)
                    .collect(Collectors.toList());
            dto.setDetails(detailDTOs);
        }

        return dto;
    }

    private PurchaseOrderDetailDTO mapDetailToDTO(PurchaseOrderDetail detail) {
        String displayName = "";
        if (detail.getProduct() != null) {
            displayName = detail.getProduct().getSku() + " - " + detail.getProduct().getProductName();
        }
        return PurchaseOrderDetailDTO.builder()
                .poDetailId(detail.getPoDetailId())
                .productId(detail.getProduct() != null ? detail.getProduct().getProductId() : null)
                .productDisplayName(displayName)
                .uom(detail.getUom())
                .orderedQty(detail.getOrderedQty())
                .build();
    }

    // ==================== Mapping: DTO -> Entity ====================

    private PurchaseOrder mapToEntity(PurchaseOrderDTO dto, User currentUser) {
        PurchaseOrder po;

        if (dto.getPoId() != null) {
            // Update existing PO
            po = poRepository.findById(dto.getPoId())
                    .orElseThrow(() -> new IllegalArgumentException("Purchase Order not found: " + dto.getPoId()));
            // Only allow editing Draft or Rejected POs
            if (!"Draft".equals(po.getPoStatus()) && !"Rejected".equals(po.getPoStatus())) {
                throw new IllegalArgumentException("Only Draft or Rejected POs can be edited.");
            }
            // Clear old details
            if (po.getDetails() != null) {
                po.getDetails().clear();
            }
        } else {
            // Create new PO
            po = new PurchaseOrder();
            po.setPoNumber(generatePONumber());
            po.setWarehouse(currentUser.getWarehouse());
            po.setDetails(new ArrayList<>());
        }

        // Set supplier
        if (dto.getSupplierId() != null) {
            Partner supplier = partnerRepository.findById(dto.getSupplierId())
                    .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + dto.getSupplierId()));
            po.setSupplier(supplier);
        }

        // Map detail lines (filter empty ones)
        List<PurchaseOrderDetailDTO> validDetails = filterEmptyDetails(dto.getDetails());
        for (PurchaseOrderDetailDTO detailDTO : validDetails) {
            PurchaseOrderDetail detail = new PurchaseOrderDetail();
            detail.setPurchaseOrder(po);
            Product product = productRepository.findById(detailDTO.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + detailDTO.getProductId()));
            detail.setProduct(product);
            detail.setUom(detailDTO.getUom());
            detail.setOrderedQty(detailDTO.getOrderedQty());
            detail.setReceivedQty(0);
            po.getDetails().add(detail);
        }

        return po;
    }

    // ==================== Helpers ====================

    private List<PurchaseOrderDetailDTO> filterEmptyDetails(List<PurchaseOrderDetailDTO> details) {
        if (details == null) return new ArrayList<>();
        return details.stream()
                .filter(d -> d.getProductId() != null)
                .collect(Collectors.toList());
    }
}
