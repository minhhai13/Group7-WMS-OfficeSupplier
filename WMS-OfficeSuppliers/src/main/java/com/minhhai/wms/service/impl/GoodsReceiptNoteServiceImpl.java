package com.minhhai.wms.service.impl;

import com.minhhai.wms.dto.GoodsReceiptDetailDTO;
import com.minhhai.wms.dto.GoodsReceiptNoteDTO;
import com.minhhai.wms.entity.*;
import com.minhhai.wms.repository.*;
import com.minhhai.wms.service.GoodsReceiptNoteService;
import com.minhhai.wms.service.SalesOrderService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class GoodsReceiptNoteServiceImpl implements GoodsReceiptNoteService {

    private final GoodsReceiptNoteRepository grnRepository;
    private final GoodsReceiptDetailRepository grnDetailRepository;
    private final PurchaseOrderRepository poRepository;
    private final TransferOrderRepository toRepository;
    private final StockBatchRepository stockBatchRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductUoMConversionRepository uomConversionRepository;
    private final BinRepository binRepository;
    private final PurchaseRequestRepository prRepository;
    private final SalesOrderService soService;

    public GoodsReceiptNoteServiceImpl(
            GoodsReceiptNoteRepository grnRepository,
            GoodsReceiptDetailRepository grnDetailRepository,
            PurchaseOrderRepository poRepository,
            TransferOrderRepository toRepository,
            StockBatchRepository stockBatchRepository,
            StockMovementRepository stockMovementRepository,
            ProductUoMConversionRepository uomConversionRepository,
            BinRepository binRepository,
            PurchaseRequestRepository prRepository,
            @Lazy SalesOrderService soService) {
        this.grnRepository = grnRepository;
        this.grnDetailRepository = grnDetailRepository;
        this.poRepository = poRepository;
        this.toRepository = toRepository;
        this.stockBatchRepository = stockBatchRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.uomConversionRepository = uomConversionRepository;
        this.binRepository = binRepository;
        this.prRepository = prRepository;
        this.soService = soService;
    }

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

    @Override
    public String postGRN(Integer grnId, List<GoodsReceiptDetailDTO> receivedDetails) {
        GoodsReceiptNote grn = grnRepository.findById(grnId)
                .orElseThrow(() -> new IllegalArgumentException("GRN not found: " + grnId));

        if (!"Draft".equals(grn.getGrStatus())) {
            throw new IllegalArgumentException("Chỉ có thể ghi sổ phiếu GRN ở trạng thái Draft.");
        }

        Map<Integer, Integer> receivedQtyMap = new HashMap<>();
        for (GoodsReceiptDetailDTO dto : receivedDetails) {
            if (dto.getGrDetailId() != null && dto.getReceivedQty() != null) {
                receivedQtyMap.put(dto.getGrDetailId(), dto.getReceivedQty());
            }
        }

        if (grn.getPurchaseOrder() != null) {
            return postGRNForPO(grn, receivedQtyMap);
        } else if (grn.getTransferOrder() != null) {
            return postGRNForTO(grn, receivedQtyMap);
        }
        throw new IllegalArgumentException("GRN không liên kết với đơn hàng hợp lệ.");
    }

    private String postGRNForPO(GoodsReceiptNote grn, Map<Integer, Integer> receivedQtyMap) {
        PurchaseOrder po = grn.getPurchaseOrder();
        Warehouse warehouse = grn.getWarehouse();
        int totalInputQty = 0;

        for (GoodsReceiptDetail grnDetail : grn.getDetails()) {
            Integer inputReceivedQty = receivedQtyMap.getOrDefault(grnDetail.getGrDetailId(), 0);
            PurchaseOrderDetail poDetail = grnDetail.getPurchaseOrderDetail();

            if (inputReceivedQty < 0) throw new IllegalArgumentException("Số lượng thực nhận không được âm.");
            int remainingQty = poDetail.getOrderedQty() - poDetail.getReceivedQty();
            if (inputReceivedQty > remainingQty) throw new IllegalArgumentException("Số lượng thực nhận vượt quá số lượng thiếu.");

            totalInputQty += inputReceivedQty;
            grnDetail.setReceivedQty(inputReceivedQty);

            if (inputReceivedQty > 0) {
                Product product = grnDetail.getProduct();
                int baseQty = BigDecimal.valueOf(inputReceivedQty).multiply(getConversionFactor(product, grnDetail.getUom())).intValue();

                StockBatch stockBatch = stockBatchRepository.findByWarehouse_WarehouseIdAndProduct_ProductIdAndBin_BinIdAndBatchNumber(
                        warehouse.getWarehouseId(), product.getProductId(), grnDetail.getBin().getBinId(), grnDetail.getBatchNumber())
                        .orElseGet(() -> StockBatch.builder().warehouse(warehouse).product(product).bin(grnDetail.getBin())
                                .batchNumber(grnDetail.getBatchNumber()).arrivalDateTime(LocalDateTime.now())
                                .qtyAvailable(0).qtyReserved(0).qtyInTransit(0).uom(product.getBaseUoM()).build());

                stockBatch.setQtyAvailable(stockBatch.getQtyAvailable() + baseQty);
                stockBatchRepository.save(stockBatch);

                stockMovementRepository.save(StockMovement.builder().warehouse(warehouse).product(product).bin(grnDetail.getBin()).batchNumber(grnDetail.getBatchNumber()).movementType("Receipt").stockType("Physical").quantity(baseQty).uom(product.getBaseUoM()).balanceAfter(stockBatch.getQtyAvailable()).build());
                poDetail.setReceivedQty(poDetail.getReceivedQty() + inputReceivedQty);
            }
        }

        if (totalInputQty == 0) throw new IllegalArgumentException("Vui lòng nhập số lượng thực nhận.");
        grn.setGrStatus("Posted"); grnRepository.save(grn);

        boolean allComplete = true;
        for (PurchaseOrderDetail poDetail : po.getDetails()) if (poDetail.getReceivedQty() < poDetail.getOrderedQty()) allComplete = false;

        if (allComplete) {
            po.setPoStatus("Completed"); poRepository.save(po);
            return handlePOCompletionLoopback(po, grn);
        } else {
            po.setPoStatus("Incomplete"); poRepository.save(po);
            String backOrderGrnNumber = createBackOrderGRN(po, grn);
            return "Phiếu GRN " + grn.getGrnNumber() + " đã ghi sổ. Phiếu bù " + backOrderGrnNumber + " đã được tạo tự động.";
        }
    }

    private String postGRNForTO(GoodsReceiptNote grn, Map<Integer, Integer> receivedQtyMap) {
        TransferOrder to = grn.getTransferOrder();
        Warehouse warehouse = grn.getWarehouse();
        int totalInputQty = 0;

        for (GoodsReceiptDetail grnDetail : grn.getDetails()) {
            Integer inputReceivedQty = receivedQtyMap.getOrDefault(grnDetail.getGrDetailId(), 0);
            TransferOrderDetail toDetail = grnDetail.getTransferOrderDetail();

            if (inputReceivedQty < 0) throw new IllegalArgumentException("Số lượng thực nhận không được âm.");
            int remainingQty = toDetail.getIssuedQty() - toDetail.getReceivedQty();
            if (inputReceivedQty > remainingQty) throw new IllegalArgumentException("Số lượng nhận không được vượt quá xuất.");

            totalInputQty += inputReceivedQty;
            grnDetail.setReceivedQty(inputReceivedQty);

            if (inputReceivedQty > 0) {
                Product product = grnDetail.getProduct();
                int baseQty = BigDecimal.valueOf(inputReceivedQty).multiply(getConversionFactor(product, grnDetail.getUom())).intValue();

                List<StockBatch> transitBatches = stockBatchRepository.findByWarehouse_WarehouseIdAndProduct_ProductIdOrderByArrivalDateTimeAsc(warehouse.getWarehouseId(), product.getProductId());
                StockBatch transitBatch = transitBatches.stream()
                        .filter(b -> b.getBatchNumber().equals(grnDetail.getBatchNumber()) && b.getQtyInTransit() >= baseQty)
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hàng đang đi đường cho lô " + grnDetail.getBatchNumber()));

                transitBatch.setQtyInTransit(transitBatch.getQtyInTransit() - baseQty);
                stockBatchRepository.save(transitBatch);

                StockBatch targetBatch;
                if (transitBatch.getBin().getBinId().equals(grnDetail.getBin().getBinId())) {
                    targetBatch = transitBatch;
                } else {
                    targetBatch = stockBatchRepository.findByWarehouse_WarehouseIdAndProduct_ProductIdAndBin_BinIdAndBatchNumber(
                        warehouse.getWarehouseId(), product.getProductId(), grnDetail.getBin().getBinId(), grnDetail.getBatchNumber())
                        .orElseGet(() -> StockBatch.builder()
                            .warehouse(warehouse).product(product).bin(grnDetail.getBin())
                            .batchNumber(grnDetail.getBatchNumber()).arrivalDateTime(LocalDateTime.now())
                            .qtyAvailable(0).qtyReserved(0).qtyInTransit(0).uom(product.getBaseUoM()).build());
                }

                targetBatch.setQtyAvailable(targetBatch.getQtyAvailable() + baseQty);
                stockBatchRepository.save(targetBatch);

                stockMovementRepository.save(StockMovement.builder().warehouse(warehouse).product(product).bin(grnDetail.getBin()).batchNumber(grnDetail.getBatchNumber()).movementType("Transfer-In").stockType("Physical").quantity(baseQty).uom(product.getBaseUoM()).balanceAfter(targetBatch.getQtyAvailable()).build());
                toDetail.setReceivedQty(toDetail.getReceivedQty() + inputReceivedQty);
            }
        }

        if (totalInputQty == 0) throw new IllegalArgumentException("Vui lòng nhập số lượng thực nhận.");
        grn.setGrStatus("Posted"); grnRepository.save(grn);

        boolean allComplete = true;
        for (TransferOrderDetail toD : to.getDetails()) if (toD.getReceivedQty() < toD.getRequestedQty()) allComplete = false;

        if (allComplete) {
            to.setStatus("Completed"); toRepository.save(to);
            return "Phiếu GRN " + grn.getGrnNumber() + " đã ghi sổ. Lệnh chuyển kho " + to.getToNumber() + " đã hoàn thành.";
        } else {
            toRepository.save(to);
            String backOrder = createBackOrderGRNForTO(to, grn);
            return "Phiếu GRN " + grn.getGrnNumber() + " đã ghi sổ. Phiếu bù " + backOrder + " đã được tạo tự động.";
        }
    }

    private String handlePOCompletionLoopback(PurchaseOrder po, GoodsReceiptNote grn) {
        StringBuilder message = new StringBuilder();
        message.append("Phiếu GRN ").append(grn.getGrnNumber()).append(" đã ghi sổ. Đơn hàng ").append(po.getPoNumber()).append(" hoàn thành.");

        List<PurchaseRequest> linkedPRs = prRepository.findByPurchaseOrder_PoId(po.getPoId());
        Set<SalesOrder> affectedSOs = new HashSet<>();
        for (PurchaseRequest pr : linkedPRs) {
            if (!"Completed".equals(pr.getStatus())) { pr.setStatus("Completed"); prRepository.save(pr); }
            if (pr.getRelatedSalesOrder() != null) affectedSOs.add(pr.getRelatedSalesOrder());
        }

        for (SalesOrder so : affectedSOs) {
            if (!"Waiting for Stock".equals(so.getSoStatus())) continue;
            List<PurchaseRequest> allSOPRs = prRepository.findByRelatedSalesOrder_SoIdAndStatusIn(so.getSoId(), List.of("Pending", "Approved", "Converted", "Completed"));
            boolean allPRsCompleted = allSOPRs.stream().allMatch(p -> "Completed".equals(p.getStatus()));
            if (allPRsCompleted) {
                try {
                    String ginNumber = soService.approveSO(so.getSoId(), null);
                    message.append(" | SO ").append(so.getSoNumber()).append(" duyệt + giữ hàng thành công (GIN ").append(ginNumber).append(").");
                } catch (IllegalArgumentException e) {
                    message.append(" | SO ").append(so.getSoNumber()).append(" lỗi: ").append(e.getMessage());
                }
            }
        }
        return message.toString();
    }

    private String createBackOrderGRN(PurchaseOrder po, GoodsReceiptNote postedGrn) {
        String grnNumber = generateGRNNumber();
        GoodsReceiptNote newGrn = new GoodsReceiptNote();
        newGrn.setGrnNumber(grnNumber); newGrn.setPurchaseOrder(po); newGrn.setWarehouse(po.getWarehouse());
        newGrn.setGrStatus("Draft"); newGrn.setDetails(new ArrayList<>());

        for (PurchaseOrderDetail poDetail : po.getDetails()) {
            int remainingQty = poDetail.getOrderedQty() - poDetail.getReceivedQty();
            if (remainingQty <= 0) continue;
            Product product = poDetail.getProduct();
            String batchNumber = findOriginalBatchNumber(postedGrn, poDetail);
            BigDecimal conversionFactor = getConversionFactor(product, poDetail.getUom());
            BigDecimal incomingWeight = BigDecimal.valueOf(remainingQty).multiply(conversionFactor).multiply(product.getUnitWeight());
            Bin allocatedBin = allocateBin(po.getWarehouse().getWarehouseId(), incomingWeight, product.getProductName());

            GoodsReceiptDetail newDetail = new GoodsReceiptDetail();
            newDetail.setGoodsReceiptNote(newGrn); newDetail.setPurchaseOrderDetail(poDetail); newDetail.setProduct(product);
            newDetail.setReceivedQty(0); newDetail.setUom(poDetail.getUom());
            newDetail.setBatchNumber(batchNumber); newDetail.setBin(allocatedBin);
            newGrn.getDetails().add(newDetail);
        }
        grnRepository.save(newGrn);
        return grnNumber;
    }

    private String createBackOrderGRNForTO(TransferOrder to, GoodsReceiptNote postedGrn) {
        String grnNumber = generateGRNNumber();
        GoodsReceiptNote newGrn = new GoodsReceiptNote();
        newGrn.setGrnNumber(grnNumber); newGrn.setTransferOrder(to); newGrn.setWarehouse(to.getDestinationWarehouse());
        newGrn.setGrStatus("Draft"); newGrn.setDetails(new ArrayList<>());

        for (TransferOrderDetail toDetail : to.getDetails()) {
            int remainingQty = toDetail.getIssuedQty() - toDetail.getReceivedQty();
            if (remainingQty <= 0) continue;
            Product product = toDetail.getProduct();
            GoodsReceiptDetail old = postedGrn.getDetails().stream().filter(d -> d.getTransferOrderDetail().equals(toDetail)).findFirst().orElse(null);
            
            BigDecimal conversionFactor = getConversionFactor(product, toDetail.getUom());
            BigDecimal incomingWeight = BigDecimal.valueOf(remainingQty).multiply(conversionFactor).multiply(product.getUnitWeight());
            Bin allocatedBin = allocateBin(to.getDestinationWarehouse().getWarehouseId(), incomingWeight, product.getProductName());

            GoodsReceiptDetail newDetail = new GoodsReceiptDetail();
            newDetail.setGoodsReceiptNote(newGrn); newDetail.setTransferOrderDetail(toDetail); newDetail.setProduct(product);
            newDetail.setReceivedQty(0); newDetail.setUom(toDetail.getUom());
            newDetail.setBatchNumber(old != null ? old.getBatchNumber() : ""); newDetail.setBin(allocatedBin);
            newGrn.getDetails().add(newDetail);
        }
        grnRepository.save(newGrn);
        return grnNumber;
    }

    private String findOriginalBatchNumber(GoodsReceiptNote grn, PurchaseOrderDetail poDetail) {
        for (GoodsReceiptDetail d : grn.getDetails()) {
            if (d.getPurchaseOrderDetail() != null && d.getPurchaseOrderDetail().getPoDetailId().equals(poDetail.getPoDetailId())) {
                return d.getBatchNumber();
            }
        }
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "BATCH-" + dateStr + "-" + grn.getPurchaseOrder().getPoNumber() + "-P" + poDetail.getProduct().getProductId();
    }

    private BigDecimal getConversionFactor(Product product, String uom) {
        if (uom.equals(product.getBaseUoM())) return BigDecimal.ONE;
        return uomConversionRepository.findByProduct_ProductId(product.getProductId()).stream()
                .filter(conv -> conv.getFromUoM().equals(uom)).findFirst().map(conv -> BigDecimal.valueOf(conv.getConversionFactor())).orElse(BigDecimal.ONE);
    }

    private Bin allocateBin(Integer warehouseId, BigDecimal incomingWeight, String productName) {
        List<Bin> activeBins = binRepository.findByWarehouseWarehouseIdAndIsActive(warehouseId, true);
        for (Bin bin : activeBins) {
            BigDecimal currentWeight = BigDecimal.ZERO;
            for (StockBatch batch : stockBatchRepository.findByBinBinId(bin.getBinId())) {
                currentWeight = currentWeight.add(batch.getProduct().getUnitWeight().multiply(BigDecimal.valueOf(batch.getQtyAvailable() + batch.getQtyInTransit())));
            }
            if (bin.getMaxWeight().subtract(currentWeight).compareTo(incomingWeight) >= 0) return bin;
        }
        throw new IllegalArgumentException("Kho không đủ sức chứa cho mặt hàng '" + productName + "'.");
    }

    private String generateGRNNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "GRN-" + dateStr + "-";
        String maxNumber = grnRepository.findMaxGrnNumber(prefix);
        return prefix + String.format("%03d", maxNumber != null ? Integer.parseInt(maxNumber.substring(prefix.length())) + 1 : 1);
    }

    private GoodsReceiptNoteDTO mapToListDTO(GoodsReceiptNote grn) {
        String orderNumber = grn.getPurchaseOrder() != null ? grn.getPurchaseOrder().getPoNumber() : grn.getTransferOrder().getToNumber();
        String supplierName = grn.getPurchaseOrder() != null && grn.getPurchaseOrder().getSupplier() != null ? grn.getPurchaseOrder().getSupplier().getPartnerName() : (grn.getTransferOrder() != null ? "Warehouse " + grn.getTransferOrder().getSourceWarehouse().getWarehouseName() : null);
        return GoodsReceiptNoteDTO.builder()
                .grnId(grn.getGrnId()).grnNumber(grn.getGrnNumber()).poNumber(orderNumber).supplierName(supplierName)
                .warehouseName(grn.getWarehouse() != null ? grn.getWarehouse().getWarehouseName() : null).grStatus(grn.getGrStatus()).build();
    }

    private GoodsReceiptNoteDTO mapToFullDTO(GoodsReceiptNote grn) {
        GoodsReceiptNoteDTO dto = mapToListDTO(grn);
        if (grn.getDetails() != null) dto.setDetails(grn.getDetails().stream().map(this::mapDetailToDTO).collect(Collectors.toList()));
        return dto;
    }

    private GoodsReceiptDetailDTO mapDetailToDTO(GoodsReceiptDetail detail) {
        String displayName = detail.getProduct() != null ? detail.getProduct().getSku() + " - " + detail.getProduct().getProductName() : "";
        Integer orderedQty = null;
        if (detail.getPurchaseOrderDetail() != null) orderedQty = detail.getPurchaseOrderDetail().getOrderedQty() - detail.getPurchaseOrderDetail().getReceivedQty();
        else if (detail.getTransferOrderDetail() != null) orderedQty = detail.getTransferOrderDetail().getIssuedQty() - detail.getTransferOrderDetail().getReceivedQty();

        return GoodsReceiptDetailDTO.builder()
                .grDetailId(detail.getGrDetailId()).productDisplayName(displayName).uom(detail.getUom())
                .orderedQty(orderedQty).receivedQty(detail.getReceivedQty()).batchNumber(detail.getBatchNumber())
                .binLocation(detail.getBin() != null ? detail.getBin().getBinLocation() : null).build();
    }
}
