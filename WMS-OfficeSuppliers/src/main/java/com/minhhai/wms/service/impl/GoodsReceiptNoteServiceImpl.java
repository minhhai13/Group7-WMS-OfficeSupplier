package com.minhhai.wms.service.impl;

import com.minhhai.wms.dto.GoodsReceiptDetailDTO;
import com.minhhai.wms.dto.GoodsReceiptNoteDTO;
import com.minhhai.wms.entity.*;
import com.minhhai.wms.repository.*;
import com.minhhai.wms.service.GoodsReceiptNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GoodsReceiptNoteServiceImpl implements GoodsReceiptNoteService {

    private final GoodsReceiptNoteRepository grnRepository;
    private final GoodsReceiptDetailRepository grnDetailRepository;
    private final PurchaseOrderRepository poRepository;
    private final StockBatchRepository stockBatchRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductUoMConversionRepository uomConversionRepository;
    private final BinRepository binRepository;

    // ==================== Query ====================

    @Override
    @Transactional(readOnly = true)
    public List<GoodsReceiptNoteDTO> getGRNsByWarehouse(Integer warehouseId, String status) {
        List<GoodsReceiptNote> grns;
        if (status != null && !status.isBlank()) {
            grns = grnRepository.findByWarehouse_WarehouseIdAndGrStatus(warehouseId, status);
        } else {
            grns = grnRepository.findByWarehouse_WarehouseId(warehouseId);
        }
        return grns.stream().map(this::mapToListDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public GoodsReceiptNoteDTO getGRNById(Integer grnId) {
        GoodsReceiptNote grn = grnRepository.findById(grnId)
                .orElseThrow(() -> new IllegalArgumentException("GRN not found: " + grnId));
        return mapToFullDTO(grn);
    }

    // ==================== Post GRN ====================

    @Override
    public String postGRN(Integer grnId, List<GoodsReceiptDetailDTO> receivedDetails) {
        GoodsReceiptNote grn = grnRepository.findById(grnId)
                .orElseThrow(() -> new IllegalArgumentException("GRN not found: " + grnId));

        if (!"Draft".equals(grn.getGrStatus())) {
            throw new IllegalArgumentException("Chỉ có thể ghi sổ phiếu GRN ở trạng thái Draft.");
        }

        PurchaseOrder po = grn.getPurchaseOrder();
        Warehouse warehouse = grn.getWarehouse();

        // Build a lookup map: grDetailId -> receivedQty from form
        Map<Integer, Integer> receivedQtyMap = new HashMap<>();
        for (GoodsReceiptDetailDTO dto : receivedDetails) {
            if (dto.getGrDetailId() != null && dto.getReceivedQty() != null) {
                receivedQtyMap.put(dto.getGrDetailId(), dto.getReceivedQty());
            }
        }

        // Process each GRN detail line
        int totalInputQty = 0;
        for (GoodsReceiptDetail grnDetail : grn.getDetails()) {
            Integer inputReceivedQty = receivedQtyMap.getOrDefault(grnDetail.getGrDetailId(), 0);
            PurchaseOrderDetail poDetail = grnDetail.getPurchaseOrderDetail();

            // Validate: receivedQty must be >= 0
            if (inputReceivedQty < 0) {
                throw new IllegalArgumentException("Số lượng thực nhận không được âm.");
            }
            // Validate: receivedQty <= remaining qty (orderedQty - already received)
            int remainingQty = poDetail.getOrderedQty() - poDetail.getReceivedQty();
            if (inputReceivedQty > remainingQty) {
                throw new IllegalArgumentException(
                        "Số lượng thực nhận không được lớn hơn số lượng còn thiếu ("
                        + remainingQty + ") cho sản phẩm '" + poDetail.getProduct().getProductName() + "'.");
            }
            totalInputQty += inputReceivedQty;

            // Update GRN detail receivedQty
            grnDetail.setReceivedQty(inputReceivedQty);

            if (inputReceivedQty > 0) {
                Product product = grnDetail.getProduct();

                // Convert receivedQty to base UoM for stock
                BigDecimal conversionFactor = getConversionFactor(product, grnDetail.getUom());
                int baseQty = BigDecimal.valueOf(inputReceivedQty)
                        .multiply(conversionFactor).intValue();

                // Upsert StockBatch
                Optional<StockBatch> existingBatch = stockBatchRepository
                        .findByWarehouse_WarehouseIdAndProduct_ProductIdAndBin_BinIdAndBatchNumber(
                                warehouse.getWarehouseId(),
                                product.getProductId(),
                                grnDetail.getBin().getBinId(),
                                grnDetail.getBatchNumber());

                StockBatch stockBatch;
                if (existingBatch.isPresent()) {
                    stockBatch = existingBatch.get();
                    stockBatch.setQtyAvailable(stockBatch.getQtyAvailable() + baseQty);
                } else {
                    stockBatch = StockBatch.builder()
                            .warehouse(warehouse)
                            .product(product)
                            .bin(grnDetail.getBin())
                            .batchNumber(grnDetail.getBatchNumber())
                            .arrivalDateTime(LocalDateTime.now())
                            .qtyAvailable(baseQty)
                            .qtyReserved(0)
                            .qtyInTransit(0)
                            .uom(product.getBaseUoM())
                            .build();
                }
                stockBatch = stockBatchRepository.save(stockBatch);

                // Create StockMovement log
                StockMovement movement = StockMovement.builder()
                        .warehouse(warehouse)
                        .product(product)
                        .bin(grnDetail.getBin())
                        .batchNumber(grnDetail.getBatchNumber())
                        .movementType("Receipt")
                        .stockType("Physical")
                        .quantity(baseQty)
                        .uom(product.getBaseUoM())
                        .balanceAfter(stockBatch.getQtyAvailable())
                        .build();
                stockMovementRepository.save(movement);

                // Update PO detail receivedQty (cumulative across all GRNs)
                poDetail.setReceivedQty(poDetail.getReceivedQty() + inputReceivedQty);
            }
        }

        // Bug fix #1: Prevent posting with all receivedQty = 0 (infinite back-order loop)
        if (totalInputQty == 0) {
            throw new IllegalArgumentException("Vui lòng nhập số lượng thực nhận cho ít nhất một mặt hàng.");
        }

        // GRN → Posted
        grn.setGrStatus("Posted");
        grnRepository.save(grn);

        // Check PO completion — compare all PO details
        boolean allComplete = true;
        boolean anyShortage = false;
        for (PurchaseOrderDetail poDetail : po.getDetails()) {
            if (poDetail.getReceivedQty() < poDetail.getOrderedQty()) {
                allComplete = false;
                anyShortage = true;
            }
        }

        String resultMessage;
        if (allComplete) {
            po.setPoStatus("Completed");
            poRepository.save(po);
            resultMessage = "Phiếu GRN " + grn.getGrnNumber() + " đã ghi sổ thành công. Đơn hàng " + po.getPoNumber() + " đã hoàn thành.";
        } else {
            po.setPoStatus("Incomplete");
            poRepository.save(po);

            // Back-order: create new Draft GRN for remaining quantities
            String backOrderGrnNumber = createBackOrderGRN(po, grn);
            resultMessage = "Phiếu GRN " + grn.getGrnNumber() + " đã ghi sổ. Đơn hàng " + po.getPoNumber()
                    + " chưa nhận đủ. Phiếu bù " + backOrderGrnNumber + " đã được tạo tự động.";
        }

        return resultMessage;
    }

    // ==================== Back-order Logic ====================

    private String createBackOrderGRN(PurchaseOrder po, GoodsReceiptNote postedGrn) {
        String grnNumber = generateGRNNumber();
        GoodsReceiptNote newGrn = new GoodsReceiptNote();
        newGrn.setGrnNumber(grnNumber);
        newGrn.setPurchaseOrder(po);
        newGrn.setWarehouse(po.getWarehouse());
        newGrn.setGrStatus("Draft");
        newGrn.setDetails(new ArrayList<>());

        for (PurchaseOrderDetail poDetail : po.getDetails()) {
            int remainingQty = poDetail.getOrderedQty() - poDetail.getReceivedQty();
            if (remainingQty <= 0) continue;

            Product product = poDetail.getProduct();

            // Keep the same BatchNumber from the original GRN
            String batchNumber = findOriginalBatchNumber(postedGrn, poDetail);

            // Re-allocate Bin based on current actual capacity
            BigDecimal conversionFactor = getConversionFactor(product, poDetail.getUom());
            BigDecimal incomingWeight = BigDecimal.valueOf(remainingQty)
                    .multiply(conversionFactor)
                    .multiply(product.getUnitWeight());
            Bin allocatedBin = allocateBin(po.getWarehouse().getWarehouseId(), incomingWeight, product.getProductName());

            GoodsReceiptDetail newDetail = new GoodsReceiptDetail();
            newDetail.setGoodsReceiptNote(newGrn);
            newDetail.setPurchaseOrderDetail(poDetail);
            newDetail.setProduct(product);
            newDetail.setReceivedQty(0);
            newDetail.setUom(poDetail.getUom());
            newDetail.setBatchNumber(batchNumber);
            newDetail.setBin(allocatedBin);

            newGrn.getDetails().add(newDetail);
        }

        grnRepository.save(newGrn);
        return grnNumber;
    }

    private String findOriginalBatchNumber(GoodsReceiptNote grn, PurchaseOrderDetail poDetail) {
        for (GoodsReceiptDetail d : grn.getDetails()) {
            if (d.getPurchaseOrderDetail().getPoDetailId().equals(poDetail.getPoDetailId())) {
                return d.getBatchNumber();
            }
        }
        // Fallback: generate new one
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "BATCH-" + dateStr + "-" + grn.getPurchaseOrder().getPoNumber() + "-P" + poDetail.getProduct().getProductId();
    }

    // ==================== Shared Helpers (same logic as PurchaseOrderServiceImpl) ====================

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

    private BigDecimal calculateCurrentBinWeight(Bin bin) {
        BigDecimal totalWeight = BigDecimal.ZERO;

        // 1. Actual stock
        List<StockBatch> batches = stockBatchRepository.findByBinBinId(bin.getBinId());
        for (StockBatch batch : batches) {
            BigDecimal batchWeight = batch.getProduct().getUnitWeight()
                    .multiply(BigDecimal.valueOf(batch.getQtyAvailable()));
            totalWeight = totalWeight.add(batchWeight);
        }

        // 2. Virtual: Draft GRN allocations
        List<GoodsReceiptDetail> draftDetails = grnDetailRepository.findDraftGrnDetailsByBinId(bin.getBinId());
        for (GoodsReceiptDetail grnDetail : draftDetails) {
            Product product = grnDetail.getProduct();
            PurchaseOrderDetail poDetail = grnDetail.getPurchaseOrderDetail();
            BigDecimal conversionFactor = getConversionFactor(product, grnDetail.getUom());
            BigDecimal virtualWeight = BigDecimal.valueOf(poDetail.getOrderedQty() - poDetail.getReceivedQty())
                    .multiply(conversionFactor)
                    .multiply(product.getUnitWeight());
            totalWeight = totalWeight.add(virtualWeight);
        }

        return totalWeight;
    }

    // ==================== GRN Number Generation ====================

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

    // ==================== Mapping ====================

    private GoodsReceiptNoteDTO mapToListDTO(GoodsReceiptNote grn) {
        PurchaseOrder po = grn.getPurchaseOrder();
        return GoodsReceiptNoteDTO.builder()
                .grnId(grn.getGrnId())
                .grnNumber(grn.getGrnNumber())
                .poNumber(po.getPoNumber())
                .supplierName(po.getSupplier() != null ? po.getSupplier().getPartnerName() : null)
                .warehouseName(grn.getWarehouse() != null ? grn.getWarehouse().getWarehouseName() : null)
                .grStatus(grn.getGrStatus())
                .build();
    }

    private GoodsReceiptNoteDTO mapToFullDTO(GoodsReceiptNote grn) {
        GoodsReceiptNoteDTO dto = mapToListDTO(grn);
        if (grn.getDetails() != null) {
            List<GoodsReceiptDetailDTO> detailDTOs = grn.getDetails().stream()
                    .map(this::mapDetailToDTO)
                    .collect(Collectors.toList());
            dto.setDetails(detailDTOs);
        }
        return dto;
    }

    private GoodsReceiptDetailDTO mapDetailToDTO(GoodsReceiptDetail detail) {
        PurchaseOrderDetail poDetail = detail.getPurchaseOrderDetail();
        String displayName = "";
        if (detail.getProduct() != null) {
            displayName = detail.getProduct().getSku() + " - " + detail.getProduct().getProductName();
        }
        return GoodsReceiptDetailDTO.builder()
                .grDetailId(detail.getGrDetailId())
                .productDisplayName(displayName)
                .uom(detail.getUom())
                .orderedQty(poDetail != null ? poDetail.getOrderedQty() - poDetail.getReceivedQty() : null)
                .receivedQty(detail.getReceivedQty())
                .batchNumber(detail.getBatchNumber())
                .binLocation(detail.getBin() != null ? detail.getBin().getBinLocation() : null)
                .build();
    }
}
