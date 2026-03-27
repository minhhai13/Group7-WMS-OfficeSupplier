package com.minhhai.wms.service.impl;

import com.minhhai.wms.dao.BinDao;
import com.minhhai.wms.dao.GoodsReceiptDetailDao;
import com.minhhai.wms.dao.GoodsReceiptNoteDao;
import com.minhhai.wms.dao.ProductUoMConversionDao;
import com.minhhai.wms.dao.PurchaseOrderDao;
import com.minhhai.wms.dao.PurchaseOrderDetailDao;
import com.minhhai.wms.dao.PurchaseRequestDao;
import com.minhhai.wms.dao.StockBatchDao;
import com.minhhai.wms.dao.StockMovementDao;
import com.minhhai.wms.dao.TransferOrderDao;
import com.minhhai.wms.dao.TransferOrderDetailDao;
import com.minhhai.wms.dto.GoodsReceiptDetailDTO;
import com.minhhai.wms.dto.GoodsReceiptNoteDTO;
import com.minhhai.wms.entity.*;
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

    private final GoodsReceiptNoteDao grnDao;
    private final GoodsReceiptDetailDao grnDetailDao;
    private final PurchaseOrderDao poDao;
    private final PurchaseOrderDetailDao poDetailDao;
    private final TransferOrderDao toDao;
    private final TransferOrderDetailDao toDetailDao;
    private final StockBatchDao stockBatchDao;
    private final StockMovementDao stockMovementDao;
    private final ProductUoMConversionDao uomConversionDao;
    private final BinDao binDao;
    private final PurchaseRequestDao prDao;
    private final SalesOrderService soService;

    public GoodsReceiptNoteServiceImpl(
            GoodsReceiptNoteDao grnDao,
            GoodsReceiptDetailDao grnDetailDao,
            PurchaseOrderDao poDao,
            PurchaseOrderDetailDao poDetailDao,
            TransferOrderDao toDao,
            TransferOrderDetailDao toDetailDao,
            StockBatchDao stockBatchDao,
            StockMovementDao stockMovementDao,
            ProductUoMConversionDao uomConversionDao,
            BinDao binDao,
            PurchaseRequestDao prDao,
            @Lazy SalesOrderService soService) {
        this.grnDao = grnDao;
        this.grnDetailDao = grnDetailDao;
        this.poDao = poDao;
        this.poDetailDao = poDetailDao;
        this.toDao = toDao;
        this.toDetailDao = toDetailDao;
        this.stockBatchDao = stockBatchDao;
        this.stockMovementDao = stockMovementDao;
        this.uomConversionDao = uomConversionDao;
        this.binDao = binDao;
        this.prDao = prDao;
        this.soService = soService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoodsReceiptNoteDTO> getGRNsByWarehouse(Integer warehouseId, String status) {
        List<GoodsReceiptNote> grns;
        if (status != null && !status.isBlank()) {
            grns = grnDao.findByWarehouseIdAndStatus(warehouseId, status);
        } else {
            grns = grnDao.findByWarehouseId(warehouseId);
        }
        return grns.stream().map(this::mapToListDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public GoodsReceiptNoteDTO getGRNById(Integer grnId) {
        GoodsReceiptNote grn = grnDao.findById(grnId)
                .orElseThrow(() -> new IllegalArgumentException("GRN not found: " + grnId));
        grn.setDetails(grnDetailDao.findByGrnId(grnId));
        return mapToFullDTO(grn);
    }

    @Override
    public String postGRN(Integer grnId, List<GoodsReceiptDetailDTO> receivedDetails) {
        GoodsReceiptNote grn = grnDao.findById(grnId)
                .orElseThrow(() -> new IllegalArgumentException("GRN not found: " + grnId));
        grn.setDetails(grnDetailDao.findByGrnId(grnId));

        if (!"Draft".equals(grn.getGrStatus())) {
            throw new IllegalArgumentException("Chi co the ghi so phieu GRN o trang thai Draft.");
        }

        Map<Integer, Integer> receivedQtyMap = new HashMap<>();
        for (GoodsReceiptDetailDTO dto : receivedDetails) {
            if (dto.getGrDetailId() != null && dto.getReceivedQty() != null) {
                receivedQtyMap.put(dto.getGrDetailId(), dto.getReceivedQty());
            }
        }

        if (grn.getPurchaseOrder() != null) {
            PurchaseOrder po = poDao.findById(grn.getPurchaseOrder().getPoId())
                    .orElseThrow(() -> new IllegalArgumentException("Purchase Order not found: " + grn.getPurchaseOrder().getPoId()));
            po.setDetails(poDetailDao.findByPoId(po.getPoId()));
            grn.setPurchaseOrder(po);
            return postGRNForPO(grn, receivedQtyMap);
        } else if (grn.getTransferOrder() != null) {
            TransferOrder to = toDao.findById(grn.getTransferOrder().getToId())
                    .orElseThrow(() -> new IllegalArgumentException("Transfer Order not found: " + grn.getTransferOrder().getToId()));
            to.setDetails(toDetailDao.findByTransferOrderId(to.getToId()));
            grn.setTransferOrder(to);
            return postGRNForTO(grn, receivedQtyMap);
        }
        throw new IllegalArgumentException("GRN is not linked to a valid order.");
    }

    private String postGRNForPO(GoodsReceiptNote grn, Map<Integer, Integer> receivedQtyMap) {
        PurchaseOrder po = grn.getPurchaseOrder();
        Warehouse warehouse = grn.getWarehouse();
        int totalInputQty = 0;
        Map<Integer, PurchaseOrderDetail> poDetailMap = po.getDetails().stream()
                .collect(Collectors.toMap(PurchaseOrderDetail::getPoDetailId, d -> d));

        for (GoodsReceiptDetail grnDetail : grn.getDetails()) {
            Integer inputReceivedQty = receivedQtyMap.getOrDefault(grnDetail.getGrDetailId(), 0);
            PurchaseOrderDetail poDetail = poDetailMap.get(grnDetail.getPurchaseOrderDetail().getPoDetailId());
            if (poDetail == null) {
                poDetail = grnDetail.getPurchaseOrderDetail();
            }

            if (inputReceivedQty < 0) throw new IllegalArgumentException("Received quantity cannot be negative.");
            int remainingQty = poDetail.getOrderedQty() - poDetail.getReceivedQty();
            if (inputReceivedQty > remainingQty)
                throw new IllegalArgumentException("So luong thuc nhan vuot qua so luong thieu.");

            totalInputQty += inputReceivedQty;
            grnDetail.setReceivedQty(inputReceivedQty);
            grnDetailDao.updateReceivedQty(grnDetail.getGrDetailId(), inputReceivedQty);

            if (inputReceivedQty > 0) {
                Product product = grnDetail.getProduct();
                int baseQty = BigDecimal.valueOf(inputReceivedQty).multiply(getConversionFactor(product, grnDetail.getUom())).intValue();

                StockBatch stockBatch = stockBatchDao.findByWarehouseIdAndProductIdAndBinIdAndBatchNumber(
                                warehouse.getWarehouseId(), product.getProductId(), grnDetail.getBin().getBinId(), grnDetail.getBatchNumber())
                        .orElseGet(() -> StockBatch.builder().warehouse(warehouse).product(product).bin(grnDetail.getBin())
                                .batchNumber(grnDetail.getBatchNumber()).arrivalDateTime(LocalDateTime.now())
                                .qtyAvailable(0).qtyReserved(0).qtyInTransit(0).uom(product.getBaseUoM()).build());

                stockBatch.setQtyAvailable(stockBatch.getQtyAvailable() + baseQty);
                stockBatchDao.save(stockBatch);

                stockMovementDao.save(StockMovement.builder().warehouse(warehouse).product(product).bin(grnDetail.getBin()).batchNumber(grnDetail.getBatchNumber()).movementType("Receipt").stockType("Physical").quantity(baseQty).uom(product.getBaseUoM()).balanceAfter(stockBatch.getQtyAvailable()).build());
                poDetail.setReceivedQty(poDetail.getReceivedQty() + inputReceivedQty);
                poDetailDao.updateReceivedQty(poDetail.getPoDetailId(), poDetail.getReceivedQty());
            }
        }

        if (totalInputQty == 0) throw new IllegalArgumentException("Vui long nhap so luong thuc nhan.");
        grn.setGrStatus("Posted");
        grnDao.updateStatus(grn.getGrnId(), "Posted");

        boolean allComplete = true;
        for (PurchaseOrderDetail poDetail : po.getDetails())
            if (poDetail.getReceivedQty() < poDetail.getOrderedQty()) allComplete = false;

        if (allComplete) {
            po.setPoStatus("Completed");
            poDao.save(po);
            return handlePOCompletionLoopback(po, grn);
        } else {
            po.setPoStatus("Incomplete");
            poDao.save(po);
            String backOrderGrnNumber = createBackOrderGRN(po, grn);
            return "Phieu GRN " + grn.getGrnNumber() + " da ghi so. Phieu bu " + backOrderGrnNumber + " da duoc tao tu dong.";
        }
    }

    private String postGRNForTO(GoodsReceiptNote grn, Map<Integer, Integer> receivedQtyMap) {
        TransferOrder to = grn.getTransferOrder();
        Warehouse warehouse = grn.getWarehouse();
        int totalInputQty = 0;
        Map<Integer, TransferOrderDetail> toDetailMap = to.getDetails().stream()
                .collect(Collectors.toMap(TransferOrderDetail::getToDetailId, d -> d));

        for (GoodsReceiptDetail grnDetail : grn.getDetails()) {

            Integer input = receivedQtyMap.getOrDefault(grnDetail.getGrDetailId(), 0);
            TransferOrderDetail toDetail = toDetailMap.get(grnDetail.getTransferOrderDetail().getToDetailId());
            if (toDetail == null) {
                toDetail = grnDetail.getTransferOrderDetail();
            }

            if (input < 0) throw new IllegalArgumentException("Received quantity cannot be negative.");

            int remaining = grnDetail.getExpectedQty() != null ? grnDetail.getExpectedQty() : toDetail.getIssuedQty() - toDetail.getReceivedQty();
            if (input > remaining) throw new IllegalArgumentException("Received quantity exceeds expected");

            totalInputQty += input;
            grnDetail.setReceivedQty(input);
            grnDetailDao.updateReceivedQty(grnDetail.getGrDetailId(), input);

            if (input > 0) {
                Product product = grnDetail.getProduct();

                int baseQty = BigDecimal.valueOf(input)
                        .multiply(getConversionFactor(product, grnDetail.getUom()))
                        .intValue();

                StockBatch transitBatch = stockBatchDao
                        .findByWarehouseIdAndProductIdOrderByArrivalDateTimeAsc(
                                warehouse.getWarehouseId(), product.getProductId())
                        .stream()
                        .filter(b -> b.getBatchNumber().equals(grnDetail.getBatchNumber())
                                && b.getQtyInTransit() >= baseQty)
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("No in-transit stock found"));

                transitBatch.setQtyInTransit(transitBatch.getQtyInTransit() - baseQty);
                transitBatch.setQtyAvailable(transitBatch.getQtyAvailable() + baseQty);
                stockBatchDao.save(transitBatch);
                stockMovementDao.save(StockMovement.builder()
                        .warehouse(warehouse)
                        .product(product)
                        .bin(transitBatch.getBin())
                        .batchNumber(grnDetail.getBatchNumber())
                        .movementType("Transfer-In")
                        .stockType("Physical")
                        .quantity(baseQty)
                        .uom(product.getBaseUoM())
                        .balanceAfter(transitBatch.getQtyAvailable())
                        .build());

                toDetail.setReceivedQty(toDetail.getReceivedQty() + input);
                toDetailDao.updateReceivedQty(toDetail.getToDetailId(), toDetail.getReceivedQty());
            }
        }

        if (totalInputQty == 0)
            throw new IllegalArgumentException("Quantity not entered");

        grn.setGrStatus("Posted");
        grnDao.updateStatus(grn.getGrnId(), "Posted");

        // Chỉ check các detail có trong GRN này (theo expectedQty > 0)
        boolean thisGRNFullyReceived = grn.getDetails().stream()
                .allMatch(d -> d.getReceivedQty() >= d.getExpectedQty());

        boolean allFullyComplete = to.getDetails().stream()
                .allMatch(d -> d.getReceivedQty() >= d.getRequestedQty());

        if (allFullyComplete) {
            to.setStatus("Completed");
        } else if (!thisGRNFullyReceived) {
            createBackOrderGRNForTO(to, grn);
        }
        toDao.save(to);

        return "GRN " + grn.getGrnNumber() + " posted.";
    }

    private String handlePOCompletionLoopback(PurchaseOrder po, GoodsReceiptNote grn) {
        StringBuilder message = new StringBuilder();
        message.append("Phieu GRN ").append(grn.getGrnNumber()).append(" da ghi so. Don hang ").append(po.getPoNumber()).append(" hoan thanh.");

        List<PurchaseRequest> linkedPRs = prDao.findByPurchaseOrderId(po.getPoId());

        // FIX: Use Set<Integer> (primitive IDs) instead of Set<SalesOrder> (entities).
        // Hibernate may return multiple proxy instances for the SAME SO entity
        // when a PR has multiple detail lines (JOIN duplication at DB level).
        // Since SalesOrder does NOT override equals/hashCode, Set<SalesOrder>
        // would NOT deduplicate them -> approveSO() called N times -> duplicate GINs.
        // Using Set<Integer> guarantees each SO is processed exactly once.
        Set<Integer> affectedSOIds = new HashSet<>();
        for (PurchaseRequest pr : linkedPRs) {
            if (!"Completed".equals(pr.getStatus())) {
                pr.setStatus("Completed");
                prDao.save(pr);
            }
            if (pr.getRelatedSalesOrder() != null) affectedSOIds.add(pr.getRelatedSalesOrder().getSoId());
        }

        for (Integer soId : affectedSOIds) {
            List<PurchaseRequest> allSOPRs = prDao.findByRelatedSalesOrderIdAndStatusIn(soId, List.of("Pending", "Approved", "Converted", "Completed"));
            boolean allPRsCompleted = allSOPRs.stream().allMatch(p -> "Completed".equals(p.getStatus()));
            if (allPRsCompleted) {
                try {
                    String ginNumber = soService.approveSO(soId, null);
                    if (ginNumber != null) {
                        message.append(" | SO #").append(soId).append(" duyet + giu hang thanh cong (GIN ").append(ginNumber).append(").");
                    }
                } catch (IllegalArgumentException e) {
                    message.append(" | SO #").append(soId).append(" loi: ").append(e.getMessage());
                }
            }
        }
        return message.toString();
    }

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
            String batchNumber = findOriginalBatchNumber(postedGrn, poDetail);
            BigDecimal conversionFactor = getConversionFactor(product, poDetail.getUom());
            BigDecimal incomingWeight = BigDecimal.valueOf(remainingQty).multiply(conversionFactor).multiply(product.getUnitWeight());
            Bin allocatedBin = allocateBin(po.getWarehouse().getWarehouseId(), incomingWeight, product.getProductName());

            GoodsReceiptDetail newDetail = new GoodsReceiptDetail();
            newDetail.setGoodsReceiptNote(newGrn);
            newDetail.setPurchaseOrderDetail(poDetail);
            newDetail.setProduct(product);
            newDetail.setReceivedQty(0);
            newDetail.setExpectedQty(0);
            newDetail.setUom(poDetail.getUom());
            newDetail.setBatchNumber(batchNumber);
            newDetail.setBin(allocatedBin);
            newGrn.getDetails().add(newDetail);
        }
        grnDao.save(newGrn);
        for (GoodsReceiptDetail detail : newGrn.getDetails()) {
            grnDetailDao.save(detail);
        }
        return grnNumber;
    }

    private String createBackOrderGRNForTO(TransferOrder to, GoodsReceiptNote postedGrn) {
        String grnNumber = generateGRNNumber();
        GoodsReceiptNote newGrn = new GoodsReceiptNote();
        newGrn.setGrnNumber(grnNumber);
        newGrn.setTransferOrder(to);
        newGrn.setWarehouse(to.getDestinationWarehouse());
        newGrn.setGrStatus("Draft");
        newGrn.setDetails(new ArrayList<>());

        // BUGFIX: Loop through GRN details just posted, not TO details
        for (GoodsReceiptDetail postedDetail : postedGrn.getDetails()) {
            int expected = postedDetail.getExpectedQty() != null ? postedDetail.getExpectedQty() : 0;
            int received = postedDetail.getReceivedQty() != null ? postedDetail.getReceivedQty() : 0;

            // Only track missing qty for this specific GRN
            int missingQty = expected - received;

            if (missingQty <= 0) continue;

            TransferOrderDetail toDetail = postedDetail.getTransferOrderDetail();
            Product product = postedDetail.getProduct();

            BigDecimal conversionFactor = getConversionFactor(product, postedDetail.getUom());
            BigDecimal incomingWeight = BigDecimal.valueOf(missingQty).multiply(conversionFactor).multiply(product.getUnitWeight());
            Bin allocatedBin = allocateBin(to.getDestinationWarehouse().getWarehouseId(), incomingWeight, product.getProductName());

            GoodsReceiptDetail newDetail = new GoodsReceiptDetail();
            newDetail.setGoodsReceiptNote(newGrn);
            newDetail.setTransferOrderDetail(toDetail);
            newDetail.setProduct(product);
            newDetail.setReceivedQty(0);
            newDetail.setExpectedQty(missingQty); // Set new expected qty to the missing amount
            newDetail.setUom(postedDetail.getUom());
            newDetail.setBatchNumber(postedDetail.getBatchNumber());
            newDetail.setBin(allocatedBin);

            newGrn.getDetails().add(newDetail);
        }

        // Only save if new detail lines were generated
        if (!newGrn.getDetails().isEmpty()) {
            grnDao.save(newGrn);
            for (GoodsReceiptDetail detail : newGrn.getDetails()) {
                grnDetailDao.save(detail);
            }
            return grnNumber;
        }

        return null; // No backorder needed
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
        return uomConversionDao.findByProductId(product.getProductId()).stream()
                .filter(conv -> conv.getFromUoM().equals(uom)).findFirst().map(conv -> BigDecimal.valueOf(conv.getConversionFactor())).orElse(BigDecimal.ONE);
    }

    private Bin allocateBin(Integer warehouseId, BigDecimal incomingWeight, String productName) {
        List<Bin> activeBins = binDao.findByWarehouseIdAndIsActive(warehouseId, true);
        for (Bin bin : activeBins) {
            BigDecimal currentWeight = BigDecimal.ZERO;
            for (StockBatch batch : stockBatchDao.findByBinId(bin.getBinId())) {
                currentWeight = currentWeight.add(batch.getProduct().getUnitWeight().multiply(BigDecimal.valueOf(batch.getQtyAvailable() + batch.getQtyInTransit())));
            }
            if (bin.getMaxWeight().subtract(currentWeight).compareTo(incomingWeight) >= 0) return bin;
        }
        throw new IllegalArgumentException("Insufficient warehouse capacity for item '" + productName + "'.");
    }

    private String generateGRNNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "GRN-" + dateStr + "-";
        String maxNumber = grnDao.findMaxGrnNumber(prefix);
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
        if (grn.getDetails() != null)
            dto.setDetails(grn.getDetails().stream().map(this::mapDetailToDTO).collect(Collectors.toList()));
        return dto;
    }

    private GoodsReceiptDetailDTO mapDetailToDTO(GoodsReceiptDetail detail) {
        String displayName = detail.getProduct() != null ? detail.getProduct().getSku() + " - " + detail.getProduct().getProductName() : "";
        Integer orderedQty = null;
        if (detail.getPurchaseOrderDetail() != null)
            orderedQty = detail.getPurchaseOrderDetail().getOrderedQty() - detail.getPurchaseOrderDetail().getReceivedQty();
        else if (detail.getTransferOrderDetail() != null)
            orderedQty = detail.getExpectedQty() != null ? detail.getExpectedQty() : detail.getTransferOrderDetail().getIssuedQty() - detail.getTransferOrderDetail().getReceivedQty();

        return GoodsReceiptDetailDTO.builder()
                .grDetailId(detail.getGrDetailId()).productDisplayName(displayName).uom(detail.getUom())
                .orderedQty(orderedQty).receivedQty(detail.getReceivedQty()).batchNumber(detail.getBatchNumber())
                .binLocation(detail.getBin() != null ? detail.getBin().getBinLocation() : null).build();
    }
}
