package com.minhhai.wms.service.impl;

import com.minhhai.wms.dao.*;
import com.minhhai.wms.dto.GoodsIssueDetailDTO;
import com.minhhai.wms.dto.GoodsIssueNoteDTO;
import com.minhhai.wms.entity.*;
import com.minhhai.wms.service.GoodsIssueNoteService;
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
public class GoodsIssueNoteServiceImpl implements GoodsIssueNoteService {

    private final GoodsIssueNoteDao ginDao;
    private final GoodsIssueDetailDao ginDetailDao;
    private final GoodsReceiptNoteDao grnDao;
    private final GoodsReceiptDetailDao grnDetailDao;
    private final SalesOrderDao soDao;
    private final SalesOrderDetailDao soDetailDao;
    private final TransferOrderDao toDao;
    private final TransferOrderDetailDao toDetailDao;
    private final StockBatchDao stockBatchDao;
    private final StockMovementDao stockMovementDao;
    private final ProductUoMConversionDao uomConversionDao;
    private final BinDao binDao;

    // ==================== Query Methods ====================

    @Override
    @Transactional(readOnly = true)
    public List<GoodsIssueNoteDTO> getGINsByWarehouse(Integer warehouseId, String status) {
        List<GoodsIssueNote> gins;
        if (status != null && !status.isBlank()) {
            gins = ginDao.findByWarehouseIdAndStatus(warehouseId, status);
        } else {
            gins = ginDao.findByWarehouseId(warehouseId);
        }
        return gins.stream().map(this::mapToListDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public GoodsIssueNoteDTO getGINById(Integer ginId) {
        GoodsIssueNote gin = ginDao.findById(ginId)
                .orElseThrow(() -> new IllegalArgumentException("GIN not found: " + ginId));
        gin.setDetails(ginDetailDao.findByGinId(ginId));
        return mapToFullDTO(gin);
    }

    // ==================== Post GIN (dispatcher) ====================

    @Override
    public String postGIN(Integer ginId, List<GoodsIssueDetailDTO> issuedDetails) {
        GoodsIssueNote gin = ginDao.findById(ginId)
                .orElseThrow(() -> new IllegalArgumentException("GIN not found: " + ginId));

        // Manual fetch – no Lazy Loading
        gin.setDetails(ginDetailDao.findByGinId(ginId));

        if (!"Draft".equals(gin.getGiStatus())) {
            throw new IllegalArgumentException("Only Draft GINs can be posted.");
        }

        // Build a map: giDetailId → issuedQty from the submitted form
        Map<Integer, Integer> issuedQtyMap = new HashMap<>();
        for (GoodsIssueDetailDTO dto : issuedDetails) {
            if (dto.getGiDetailId() != null && dto.getIssuedQty() != null) {
                issuedQtyMap.put(dto.getGiDetailId(), dto.getIssuedQty());
            }
        }

        if (gin.getSalesOrder() != null) {
            return postGINForSO(gin, issuedQtyMap);
        } else if (gin.getTransferOrder() != null) {
            return postGINForTO(gin, issuedQtyMap);
        }
        throw new IllegalArgumentException("GIN is not linked to a valid order.");
    }

    // ==================== Post GIN for Sales Order ====================

    private String postGINForSO(GoodsIssueNote gin, Map<Integer, Integer> issuedQtyMap) {
        // Reload SO + its details manually (no Lazy Loading)
        Integer soId = gin.getSalesOrder().getSoId();
        SalesOrder so = soDao.findById(soId)
                .orElseThrow(() -> new IllegalArgumentException("Sales Order not found: " + soId));
        so.setDetails(soDetailDao.findBySoId(soId));

        // FIX: Seed a cumulative map from the fresh SO details so that multiple GIN detail rows
        // that share the same soDetail (multi-batch FIFO) accumulate correctly.
        // Without this, each GIN detail row reads its own stale soDetail.issuedQty=0 and overwrites DB.
        Map<Integer, Integer> cumulativeIssuedQty = new HashMap<>();
        for (SalesOrderDetail d : so.getDetails()) {
            cumulativeIssuedQty.put(d.getSoDetailId(), d.getIssuedQty() != null ? d.getIssuedQty() : 0);
        }

        Warehouse warehouse = gin.getWarehouse();
        int totalIssuedInput = 0;

        for (GoodsIssueDetail ginDetail : gin.getDetails()) {
            Integer inputIssuedQty = issuedQtyMap.getOrDefault(ginDetail.getGiDetailId(), 0);
            SalesOrderDetail soDetail = ginDetail.getSalesOrderDetail();

            if (inputIssuedQty < 0) {
                throw new IllegalArgumentException("Issued quantity cannot be negative.");
            }
            // Validate against this GIN detail's planned qty (batch-level),
            // not the SO line total – prevents issuing more than reserved from one bin.
            int plannedForThisDetail = ginDetail.getPlannedQty() != null
                    ? ginDetail.getPlannedQty() : soDetail.getOrderedQty();
            int alreadyIssued = ginDetail.getIssuedQty() != null ? ginDetail.getIssuedQty() : 0;
            int remainingQty = plannedForThisDetail - alreadyIssued;
            if (inputIssuedQty > remainingQty) {
                throw new IllegalArgumentException(
                        "Số lượng xuất vượt mức cho " + ginDetail.getProduct().getProductName()
                        + " (tối đa " + remainingQty + " từ lô " + ginDetail.getBatchNumber() + ").");
            }

            totalIssuedInput += inputIssuedQty;

            if (inputIssuedQty > 0) {
                Product product = ginDetail.getProduct();
                BigDecimal cf = getConversionFactor(product, ginDetail.getUom());
                int baseQty = BigDecimal.valueOf(inputIssuedQty).multiply(cf).intValue();

                // Deduct stock from the specific batch/bin
                StockBatch stockBatch = stockBatchDao
                        .findByWarehouseIdAndProductIdAndBinIdAndBatchNumber(
                                warehouse.getWarehouseId(),
                                product.getProductId(),
                                ginDetail.getBin().getBinId(),
                                ginDetail.getBatchNumber())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Batch not found: " + ginDetail.getBatchNumber()));

                stockBatch.setQtyAvailable(stockBatch.getQtyAvailable() - baseQty);
                stockBatch.setQtyReserved(stockBatch.getQtyReserved() - baseQty);
                stockBatchDao.save(stockBatch);

                // Log stock movements
                stockMovementDao.save(StockMovement.builder()
                        .warehouse(warehouse).product(product).bin(ginDetail.getBin())
                        .batchNumber(ginDetail.getBatchNumber())
                        .movementType("Issue").stockType("Physical")
                        .quantity(baseQty).uom(product.getBaseUoM())
                        .balanceAfter(stockBatch.getQtyAvailable()).build());
                stockMovementDao.save(StockMovement.builder()
                        .warehouse(warehouse).product(product).bin(ginDetail.getBin())
                        .batchNumber(ginDetail.getBatchNumber())
                        .movementType("Issue").stockType("Reserved")
                        .quantity(baseQty).uom(product.getBaseUoM())
                        .balanceAfter(stockBatch.getQtyReserved()).build());

                // Update GIN detail issued qty
                ginDetail.setIssuedQty(inputIssuedQty);
                ginDetailDao.save(ginDetail);

                // Accumulate SO detail issued qty using the cumulative map (avoids stale object overwrite)
                int newIssuedQty = cumulativeIssuedQty.getOrDefault(soDetail.getSoDetailId(), 0) + inputIssuedQty;
                cumulativeIssuedQty.put(soDetail.getSoDetailId(), newIssuedQty);
                soDetailDao.updateIssuedQty(soDetail.getSoDetailId(), newIssuedQty);
                soDetail.setIssuedQty(newIssuedQty);
            }
        }


        if (totalIssuedInput == 0) {
            throw new IllegalArgumentException("Please enter issued quantity for all lines.");
        }

        // Mark GIN as Posted
        ginDao.updateStatus(gin.getGinId(), "Posted");

        // Check if all SO lines are fully issued
        List<SalesOrderDetail> freshDetails = soDetailDao.findBySoId(soId);
        boolean allComplete = freshDetails.stream()
                .allMatch(d -> d.getIssuedQty() >= d.getOrderedQty());

        if (allComplete) {
            soDao.save(buildSoWithStatus(so, "Completed"));
            return "GIN " + gin.getGinNumber() + " posted. Order "
                    + so.getSoNumber() + " hoàn thành.";
        } else {
            soDao.save(buildSoWithStatus(so, "Incomplete"));
            String backOrderGinNumber = createBackOrderGINForSO(so, gin, freshDetails);
            return "GIN " + gin.getGinNumber() + " posted. Back-order GIN "
                    + backOrderGinNumber + " auto-created.";
        }
    }

    // ==================== Post GIN for Transfer Order ====================

    private String postGINForTO(GoodsIssueNote gin, Map<Integer, Integer> issuedQtyMap) {
        // Reload TO + its details manually
        Integer toId = gin.getTransferOrder().getToId();
        TransferOrder to = toDao.findById(toId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer Order not found: " + toId));
        to.setDetails(toDetailDao.findByTransferOrderId(toId));

        Warehouse sourceWarehouse = gin.getWarehouse();
        Warehouse destWarehouse = to.getDestinationWarehouse();
        int totalIssuedInput = 0;

        // Track allocations for auto-GRN at destination
        List<GRNAllocation> grnAllocations = new ArrayList<>();

        for (GoodsIssueDetail ginDetail : gin.getDetails()) {
            Integer inputIssuedQty = issuedQtyMap.getOrDefault(ginDetail.getGiDetailId(), 0);
            TransferOrderDetail toDetail = ginDetail.getTransferOrderDetail();

            if (inputIssuedQty < 0) {
                throw new IllegalArgumentException("Issued quantity cannot be negative.");
            }
            int remainingQty = toDetail.getRequestedQty() - toDetail.getIssuedQty();
            if (inputIssuedQty > remainingQty) {
                throw new IllegalArgumentException("Số lượng xuất vượt mức.");
            }

            totalIssuedInput += inputIssuedQty;

            if (inputIssuedQty > 0) {
                Product product = ginDetail.getProduct();
                BigDecimal cf = getConversionFactor(product, ginDetail.getUom());
                int baseQty = BigDecimal.valueOf(inputIssuedQty).multiply(cf).intValue();

                // Deduct source stock
                StockBatch sourceBatch = stockBatchDao
                        .findByWarehouseIdAndProductIdAndBinIdAndBatchNumber(
                                sourceWarehouse.getWarehouseId(),
                                product.getProductId(),
                                ginDetail.getBin().getBinId(),
                                ginDetail.getBatchNumber())
                        .orElseThrow(() -> new IllegalArgumentException("Batch not found."));

                sourceBatch.setQtyAvailable(sourceBatch.getQtyAvailable() - baseQty);
                sourceBatch.setQtyReserved(sourceBatch.getQtyReserved() - baseQty);
                stockBatchDao.save(sourceBatch);

                stockMovementDao.save(StockMovement.builder()
                        .warehouse(sourceWarehouse).product(product).bin(ginDetail.getBin())
                        .batchNumber(ginDetail.getBatchNumber())
                        .movementType("Transfer-Out").stockType("Physical")
                        .quantity(baseQty).uom(product.getBaseUoM())
                        .balanceAfter(sourceBatch.getQtyAvailable()).build());
                stockMovementDao.save(StockMovement.builder()
                        .warehouse(sourceWarehouse).product(product).bin(ginDetail.getBin())
                        .batchNumber(ginDetail.getBatchNumber())
                        .movementType("Transfer-Out").stockType("Reserved")
                        .quantity(baseQty).uom(product.getBaseUoM())
                        .balanceAfter(sourceBatch.getQtyReserved()).build());

                // Allocate a bin at destination and create/update in-transit stock
                BigDecimal incomingWeight = BigDecimal.valueOf(baseQty)
                        .multiply(product.getUnitWeight());
                Bin destBin = allocateBin(destWarehouse.getWarehouseId(),
                        incomingWeight, product.getProductName());

                StockBatch destBatch = stockBatchDao
                        .findByWarehouseIdAndProductIdAndBinIdAndBatchNumber(
                                destWarehouse.getWarehouseId(),
                                product.getProductId(),
                                destBin.getBinId(),
                                ginDetail.getBatchNumber())
                        .orElseGet(() -> StockBatch.builder()
                                .warehouse(destWarehouse).product(product).bin(destBin)
                                .batchNumber(ginDetail.getBatchNumber())
                                .arrivalDateTime(LocalDateTime.now())
                                .qtyAvailable(0).qtyReserved(0).qtyInTransit(0)
                                .uom(product.getBaseUoM()).build());

                destBatch.setQtyInTransit(destBatch.getQtyInTransit() + baseQty);
                stockBatchDao.save(destBatch);

                // Update GIN detail
                ginDetail.setIssuedQty(inputIssuedQty);
                ginDetailDao.save(ginDetail);

                // Update TO detail issued qty
                int newIssuedQty = toDetail.getIssuedQty() + inputIssuedQty;
                toDetailDao.updateIssuedQty(toDetail.getToDetailId(), newIssuedQty);
                toDetail.setIssuedQty(newIssuedQty);

                grnAllocations.add(new GRNAllocation(
                        toDetail, product, ginDetail.getBatchNumber(),
                        destBin, ginDetail.getUom(), inputIssuedQty));
            }
        }

        if (totalIssuedInput == 0) {
            throw new IllegalArgumentException("Please enter issued quantity for all lines.");
        }

        ginDao.updateStatus(gin.getGinId(), "Posted");

        // Check if all TO lines are fully issued
        List<TransferOrderDetail> freshToDetails = toDetailDao.findByTransferOrderId(toId);
        boolean allComplete = freshToDetails.stream()
                .allMatch(d -> d.getIssuedQty() >= d.getRequestedQty());

        String toStatusMsg;
        if (allComplete) {
            to.setStatus("In-Transit");
            toDao.save(to);
            toStatusMsg = "Transfer Order " + to.getToNumber() + " is In-Transit.";
        } else {
            to.setStatus("Incomplete");
            toDao.save(to);
            String backOrder = createBackOrderGINForTO(to, gin);
            toStatusMsg = "Back-order GIN " + (backOrder != null ? backOrder : "N/A") + " created.";
        }

        // Auto-create Draft GRN for destination warehouse
        GoodsReceiptNote grn = new GoodsReceiptNote();
        grn.setGrnNumber(generateGRNNumber());
        grn.setTransferOrder(to);
        grn.setWarehouse(destWarehouse);
        grn.setGrStatus("Draft");
        grn.setDetails(new ArrayList<>());

        for (GRNAllocation alloc : grnAllocations) {
            GoodsReceiptDetail grnDetail = new GoodsReceiptDetail();
            grnDetail.setGoodsReceiptNote(grn);
            grnDetail.setTransferOrderDetail(alloc.toDetail());
            grnDetail.setProduct(alloc.product());
            grnDetail.setReceivedQty(0);
            grnDetail.setExpectedQty(alloc.issuedQty());
            grnDetail.setUom(alloc.uom());
            grnDetail.setBatchNumber(alloc.batchNumber());
            grnDetail.setBin(alloc.destBin());
            grn.getDetails().add(grnDetail);
        }

        grnDao.save(grn);
        for (GoodsReceiptDetail detail : grn.getDetails()) {
            grnDetailDao.save(detail);
        }

        return "GIN " + gin.getGinNumber() + " posted. " + toStatusMsg
                + " GRN " + grn.getGrnNumber() + " auto-created at destination warehouse.";
    }

    // ==================== Back-order GIN: Sales Order ====================

    private String createBackOrderGINForSO(SalesOrder so, GoodsIssueNote postedGin,
                                           List<SalesOrderDetail> freshDetails) {
        String ginNumber = generateGINNumber();
        GoodsIssueNote newGin = new GoodsIssueNote();
        newGin.setGinNumber(ginNumber);
        newGin.setSalesOrder(so);
        newGin.setWarehouse(so.getWarehouse());
        newGin.setGiStatus("Draft");

        ginDao.save(newGin);

        for (SalesOrderDetail soDetail : freshDetails) {
            int remaining = soDetail.getOrderedQty() - soDetail.getIssuedQty();
            if (remaining > 0) {
                // Borrow bin/batch from the last GIN detail for this SO line
                GoodsIssueDetail oldDetail = postedGin.getDetails().stream()
                        .filter(d -> d.getSalesOrderDetail() != null
                                && d.getSalesOrderDetail().getSoDetailId()
                                   .equals(soDetail.getSoDetailId()))
                        .findFirst().orElse(null);

                GoodsIssueDetail newDetail = new GoodsIssueDetail();
                newDetail.setGoodsIssueNote(newGin);
                newDetail.setSalesOrderDetail(soDetail);
                newDetail.setProduct(soDetail.getProduct());
                newDetail.setIssuedQty(0);
                newDetail.setPlannedQty(remaining);
                newDetail.setUom(soDetail.getUom());
                newDetail.setBatchNumber(oldDetail != null ? oldDetail.getBatchNumber() : "");
                newDetail.setBin(oldDetail != null ? oldDetail.getBin() : null);
                ginDetailDao.save(newDetail);
            }
        }

        return ginNumber;
    }

    // ==================== Back-order GIN: Transfer Order ====================

    private String createBackOrderGINForTO(TransferOrder to, GoodsIssueNote postedGin) {
        String ginNumber = generateGINNumber();
        GoodsIssueNote newGin = new GoodsIssueNote();
        newGin.setGinNumber(ginNumber);
        newGin.setTransferOrder(to);
        newGin.setWarehouse(to.getSourceWarehouse());
        newGin.setGiStatus("Draft");

        boolean hasBackOrderLines = false;
        for (GoodsIssueDetail postedDetail : postedGin.getDetails()) {
            int planned = postedDetail.getPlannedQty() != null ? postedDetail.getPlannedQty() : 0;
            int issued = postedDetail.getIssuedQty() != null ? postedDetail.getIssuedQty() : 0;
            int missing = planned - issued;
            if (missing > 0) {
                hasBackOrderLines = true;
                break;
            }
        }
        if (!hasBackOrderLines) return null;

        ginDao.save(newGin);

        for (GoodsIssueDetail postedDetail : postedGin.getDetails()) {
            int planned = postedDetail.getPlannedQty() != null ? postedDetail.getPlannedQty() : 0;
            int issued = postedDetail.getIssuedQty() != null ? postedDetail.getIssuedQty() : 0;
            int missing = planned - issued;

            if (missing > 0) {
                GoodsIssueDetail newDetail = new GoodsIssueDetail();
                newDetail.setGoodsIssueNote(newGin);
                newDetail.setTransferOrderDetail(postedDetail.getTransferOrderDetail());
                newDetail.setProduct(postedDetail.getProduct());
                newDetail.setUom(postedDetail.getUom());
                newDetail.setBatchNumber(postedDetail.getBatchNumber());
                newDetail.setBin(postedDetail.getBin());
                newDetail.setIssuedQty(0);
                newDetail.setPlannedQty(missing);
                ginDetailDao.save(newDetail);
            }
        }

        return ginNumber;
    }

    // ==================== Bin Allocation ====================

    private Bin allocateBin(Integer warehouseId, BigDecimal incomingWeight, String productName) {
        List<Bin> activeBins = binDao.findByWarehouseIdAndIsActive(warehouseId, true);
        for (Bin bin : activeBins) {
            BigDecimal currentWeight = BigDecimal.ZERO;

            // Actual stock weight
            for (StockBatch batch : stockBatchDao.findByBinId(bin.getBinId())) {
                currentWeight = currentWeight.add(
                        batch.getProduct().getUnitWeight()
                             .multiply(BigDecimal.valueOf(
                                     batch.getQtyAvailable() + batch.getQtyInTransit())));
            }
            // Virtual (Draft GRN) weight
            for (GoodsReceiptDetail grnDetail : grnDetailDao.findDraftByBinId(bin.getBinId())) {
                if (grnDetail.getPurchaseOrderDetail() != null) {
                    PurchaseOrderDetail poDetail = grnDetail.getPurchaseOrderDetail();
                    BigDecimal cf = getConversionFactor(grnDetail.getProduct(), grnDetail.getUom());
                    BigDecimal virtualWeight = BigDecimal.valueOf(
                            poDetail.getOrderedQty() - poDetail.getReceivedQty())
                            .multiply(cf)
                            .multiply(grnDetail.getProduct().getUnitWeight());
                    currentWeight = currentWeight.add(virtualWeight);
                }
            }

            BigDecimal remaining = bin.getMaxWeight().subtract(currentWeight);
            if (remaining.compareTo(incomingWeight) >= 0) {
                return bin;
            }
        }
        throw new IllegalArgumentException(
                "Insufficient warehouse capacity for item '" + productName + "'.");
    }

    // ==================== Number Generators ====================

    private String generateGINNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "GIN-" + dateStr + "-";
        String max = ginDao.findMaxGinNumber(prefix);
        int next = (max != null) ? Integer.parseInt(max.substring(prefix.length())) + 1 : 1;
        return prefix + String.format("%03d", next);
    }

    private String generateGRNNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "GRN-" + dateStr + "-";
        String max = grnDao.findMaxGrnNumber(prefix);
        int next = (max != null) ? Integer.parseInt(max.substring(prefix.length())) + 1 : 1;
        return prefix + String.format("%03d", next);
    }

    // ==================== UoM Conversion ====================

    private BigDecimal getConversionFactor(Product product, String uom) {
        if (uom.equals(product.getBaseUoM())) return BigDecimal.ONE;
        return uomConversionDao.findByProductId(product.getProductId()).stream()
                .filter(c -> c.getFromUoM().equals(uom))
                .findFirst()
                .map(c -> BigDecimal.valueOf(c.getConversionFactor()))
                .orElse(BigDecimal.ONE);
    }

    // ==================== Mapping helpers ====================

    private SalesOrder buildSoWithStatus(SalesOrder so, String status) {
        so.setSoStatus(status);
        return so;
    }

    private GoodsIssueNoteDTO mapToListDTO(GoodsIssueNote gin) {
        String orderNumber = null;
        String customerName = null;

        if (gin.getSalesOrder() != null) {
            orderNumber = gin.getSalesOrder().getSoNumber();
            if (gin.getSalesOrder().getCustomer() != null) {
                customerName = gin.getSalesOrder().getCustomer().getPartnerName();
            }
        } else if (gin.getTransferOrder() != null) {
            orderNumber = gin.getTransferOrder().getToNumber();
            if (gin.getTransferOrder().getDestinationWarehouse() != null) {
                customerName = "TO → " + gin.getTransferOrder().getDestinationWarehouse().getWarehouseName();
            }
        }

        return GoodsIssueNoteDTO.builder()
                .ginId(gin.getGinId())
                .ginNumber(gin.getGinNumber())
                .soNumber(orderNumber)
                .customerName(customerName)
                .warehouseName(gin.getWarehouse() != null ? gin.getWarehouse().getWarehouseName() : null)
                .giStatus(gin.getGiStatus())
                .build();
    }

    private GoodsIssueNoteDTO mapToFullDTO(GoodsIssueNote gin) {
        GoodsIssueNoteDTO dto = mapToListDTO(gin);
        if (gin.getDetails() != null) {
            dto.setDetails(gin.getDetails().stream()
                    .map(this::mapDetailToDTO).collect(Collectors.toList()));
        }
        return dto;
    }

    private GoodsIssueDetailDTO mapDetailToDTO(GoodsIssueDetail detail) {
        String displayName = detail.getProduct() != null
                ? detail.getProduct().getSku() + " - " + detail.getProduct().getProductName()
                : "";
        Integer orderedQty = null;
        if (detail.getSalesOrderDetail() != null) {
            // FIX: show planned qty for this batch/bin row, not the total SO line qty.
            // plannedQty is set per-batch when GIN is created (e.g. B1=4, B2=6).
            // This prevents the form from showing 10 for both rows.
            int planned = detail.getPlannedQty() != null ? detail.getPlannedQty() : 0;
            int issued  = detail.getIssuedQty()  != null ? detail.getIssuedQty()  : 0;
            orderedQty = planned - issued;
        } else if (detail.getTransferOrderDetail() != null) {
            orderedQty = detail.getTransferOrderDetail().getRequestedQty()
                    - detail.getTransferOrderDetail().getIssuedQty();
        }

        return GoodsIssueDetailDTO.builder()
                .giDetailId(detail.getGiDetailId())
                .productDisplayName(displayName)
                .uom(detail.getUom())
                .orderedQty(orderedQty)
                .issuedQty(detail.getIssuedQty())
                .batchNumber(detail.getBatchNumber())
                .binLocation(detail.getBin() != null ? detail.getBin().getBinLocation() : null)
                .build();
    }

    // ── Private record for GRN destination allocation tracking ──────────────
    private record GRNAllocation(TransferOrderDetail toDetail, Product product,
                                  String batchNumber, Bin destBin, String uom, int issuedQty) {}
}
