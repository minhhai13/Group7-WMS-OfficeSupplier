package com.minhhai.wms.service.impl;

import com.minhhai.wms.dto.GoodsIssueDetailDTO;
import com.minhhai.wms.dto.GoodsIssueNoteDTO;
import com.minhhai.wms.entity.*;
import com.minhhai.wms.repository.*;
import com.minhhai.wms.service.GoodsIssueNoteService;
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
public class GoodsIssueNoteServiceImpl implements GoodsIssueNoteService {

    private final GoodsIssueNoteRepository ginRepository;
    private final GoodsReceiptNoteRepository grnRepository;
    private final SalesOrderRepository soRepository;
    private final TransferOrderRepository toRepository;
    private final StockBatchRepository stockBatchRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductUoMConversionRepository uomConversionRepository;
    private final BinRepository binRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GoodsIssueNoteDTO> getGINsByWarehouse(Integer warehouseId, String status) {
        List<GoodsIssueNote> gins;
        if (status != null && !status.isBlank()) {
            gins = ginRepository.findByWarehouse_WarehouseIdAndGiStatus(warehouseId, status);
        } else {
            gins = ginRepository.findByWarehouse_WarehouseId(warehouseId);
        }
        return gins.stream().map(this::mapToListDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public GoodsIssueNoteDTO getGINById(Integer ginId) {
        GoodsIssueNote gin = ginRepository.findById(ginId)
                .orElseThrow(() -> new IllegalArgumentException("GIN not found: " + ginId));
        return mapToFullDTO(gin);
    }

    @Override
    public String postGIN(Integer ginId, List<GoodsIssueDetailDTO> issuedDetails) {
        GoodsIssueNote gin = ginRepository.findById(ginId)
                .orElseThrow(() -> new IllegalArgumentException("GIN not found: " + ginId));

        if (!"Draft".equals(gin.getGiStatus())) {
            throw new IllegalArgumentException("Chỉ có thể ghi sổ phiếu GIN ở trạng thái Draft.");
        }

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
        throw new IllegalArgumentException("GIN không liên kết với đơn hàng hợp lệ.");
    }

    private String postGINForSO(GoodsIssueNote gin, Map<Integer, Integer> issuedQtyMap) {
        SalesOrder so = gin.getSalesOrder();
        Warehouse warehouse = gin.getWarehouse();
        int totalIssuedInput = 0;
        for (GoodsIssueDetail ginDetail : gin.getDetails()) {
            Integer inputIssuedQty = issuedQtyMap.getOrDefault(ginDetail.getGiDetailId(), 0);
            SalesOrderDetail soDetail = ginDetail.getSalesOrderDetail();

            if (inputIssuedQty < 0) throw new IllegalArgumentException("Số lượng thực xuất không được âm.");
            int remainingQty = soDetail.getOrderedQty() - soDetail.getIssuedQty();
            if (inputIssuedQty > remainingQty) throw new IllegalArgumentException("Số lượng xuất vượt mức cho " + ginDetail.getProduct().getProductName());

            totalIssuedInput += inputIssuedQty;
            ginDetail.setIssuedQty(inputIssuedQty);

            if (inputIssuedQty > 0) {
                Product product = ginDetail.getProduct();
                int baseQty = BigDecimal.valueOf(inputIssuedQty).multiply(getConversionFactor(product, ginDetail.getUom())).intValue();

                StockBatch stockBatch = stockBatchRepository.findByWarehouse_WarehouseIdAndProduct_ProductIdAndBin_BinIdAndBatchNumber(
                        warehouse.getWarehouseId(), product.getProductId(), ginDetail.getBin().getBinId(), ginDetail.getBatchNumber())
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lô hàng."));

                stockBatch.setQtyAvailable(stockBatch.getQtyAvailable() - baseQty);
                stockBatch.setQtyReserved(stockBatch.getQtyReserved() - baseQty);
                stockBatchRepository.save(stockBatch);

                stockMovementRepository.save(StockMovement.builder().warehouse(warehouse).product(product).bin(ginDetail.getBin()).batchNumber(ginDetail.getBatchNumber()).movementType("Issue").stockType("Physical").quantity(baseQty).uom(product.getBaseUoM()).balanceAfter(stockBatch.getQtyAvailable()).build());
                stockMovementRepository.save(StockMovement.builder().warehouse(warehouse).product(product).bin(ginDetail.getBin()).batchNumber(ginDetail.getBatchNumber()).movementType("Issue").stockType("Reserved").quantity(baseQty).uom(product.getBaseUoM()).balanceAfter(stockBatch.getQtyReserved()).build());

                soDetail.setIssuedQty(soDetail.getIssuedQty() + inputIssuedQty);
            }
        }

        if (totalIssuedInput == 0) throw new IllegalArgumentException("Vui lòng nhập số lượng thực xuất.");

        gin.setGiStatus("Posted"); ginRepository.save(gin);
        boolean allComplete = true;
        for (SalesOrderDetail soDetail : so.getDetails()) if (soDetail.getIssuedQty() < soDetail.getOrderedQty()) allComplete = false;

        if (allComplete) {
            so.setSoStatus("Completed"); soRepository.save(so);
            return "Phiếu GIN " + gin.getGinNumber() + " đã ghi sổ. Đơn hàng " + so.getSoNumber() + " hoàn thành.";
        } else {
            so.setSoStatus("Incomplete"); soRepository.save(so);
            String backOrderGinNumber = createBackOrderGINForSO(so, gin);
            return "Phiếu GIN " + gin.getGinNumber() + " đã ghi sổ. Phiếu bù " + backOrderGinNumber + " đã được tạo.";
        }
    }

    // ==================== Post GIN for Transfer Order (auto-creates GRN) ====================

    private String postGINForTO(GoodsIssueNote gin, Map<Integer, Integer> issuedQtyMap) {
        TransferOrder to = gin.getTransferOrder();
        Warehouse sourceWarehouse = gin.getWarehouse();
        Warehouse destWarehouse = to.getDestinationWarehouse();
        int totalIssuedInput = 0;

        // Track (toDetail, product, batchNumber, destBin, uom) for auto-GRN creation
        List<GRNAllocation> grnAllocations = new ArrayList<>();

        for (GoodsIssueDetail ginDetail : gin.getDetails()) {
            Integer inputIssuedQty = issuedQtyMap.getOrDefault(ginDetail.getGiDetailId(), 0);
            TransferOrderDetail toDetail = ginDetail.getTransferOrderDetail();

            if (inputIssuedQty < 0) throw new IllegalArgumentException("Số lượng thực xuất không được âm.");
            int remainingQty = toDetail.getRequestedQty() - toDetail.getIssuedQty();
            if (inputIssuedQty > remainingQty) throw new IllegalArgumentException("Số lượng xuất vượt mức.");

            totalIssuedInput += inputIssuedQty;
            ginDetail.setIssuedQty(inputIssuedQty);

            if (inputIssuedQty > 0) {
                Product product = ginDetail.getProduct();
                int baseQty = BigDecimal.valueOf(inputIssuedQty).multiply(getConversionFactor(product, ginDetail.getUom())).intValue();

                StockBatch sourceBatch = stockBatchRepository.findByWarehouse_WarehouseIdAndProduct_ProductIdAndBin_BinIdAndBatchNumber(
                        sourceWarehouse.getWarehouseId(), product.getProductId(), ginDetail.getBin().getBinId(), ginDetail.getBatchNumber())
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lô hàng."));

                sourceBatch.setQtyAvailable(sourceBatch.getQtyAvailable() - baseQty);
                sourceBatch.setQtyReserved(sourceBatch.getQtyReserved() - baseQty);
                stockBatchRepository.save(sourceBatch);

                stockMovementRepository.save(StockMovement.builder().warehouse(sourceWarehouse).product(product).bin(ginDetail.getBin()).batchNumber(ginDetail.getBatchNumber()).movementType("Transfer-Out").stockType("Physical").quantity(baseQty).uom(product.getBaseUoM()).balanceAfter(sourceBatch.getQtyAvailable()).build());
                stockMovementRepository.save(StockMovement.builder().warehouse(sourceWarehouse).product(product).bin(ginDetail.getBin()).batchNumber(ginDetail.getBatchNumber()).movementType("Transfer-Out").stockType("Reserved").quantity(baseQty).uom(product.getBaseUoM()).balanceAfter(sourceBatch.getQtyReserved()).build());

                BigDecimal incomingWeight = BigDecimal.valueOf(baseQty).multiply(product.getUnitWeight());
                Bin destBin = allocateBin(destWarehouse.getWarehouseId(), incomingWeight, product.getProductName());

                StockBatch destBatch = stockBatchRepository.findByWarehouse_WarehouseIdAndProduct_ProductIdAndBin_BinIdAndBatchNumber(
                        destWarehouse.getWarehouseId(), product.getProductId(), destBin.getBinId(), ginDetail.getBatchNumber())
                        .orElseGet(() -> StockBatch.builder().warehouse(destWarehouse).product(product).bin(destBin).batchNumber(ginDetail.getBatchNumber()).arrivalDateTime(java.time.LocalDateTime.now()).qtyAvailable(0).qtyReserved(0).qtyInTransit(0).uom(product.getBaseUoM()).build());

                destBatch.setQtyInTransit(destBatch.getQtyInTransit() + baseQty);
                stockBatchRepository.save(destBatch);

                toDetail.setIssuedQty(toDetail.getIssuedQty() + inputIssuedQty);

                // Track the allocated dest bin/batch for GRN creation below
                grnAllocations.add(new GRNAllocation(toDetail, product, ginDetail.getBatchNumber(), destBin, ginDetail.getUom(), inputIssuedQty));
            }
        }

        if (totalIssuedInput == 0) throw new IllegalArgumentException("Vui lòng nhập số lượng thực xuất.");

        gin.setGiStatus("Posted"); ginRepository.save(gin);
        boolean allComplete = true;
        for (TransferOrderDetail toD : to.getDetails()) if (toD.getIssuedQty() < toD.getRequestedQty()) allComplete = false;

        String toStatusMsg;
        if (allComplete) {
            to.setStatus("In-Transit"); toRepository.save(to);
            toStatusMsg = "Lệnh chuyển kho " + to.getToNumber() + " đang In-Transit.";
        } else {
            to.setStatus("Incomplete"); toRepository.save(to);
            String backOrder = createBackOrderGINForTO(to, gin);
            toStatusMsg = "Phiếu bù " + backOrder + " đã được tạo.";
        }

        // ── Auto-create Draft GRN for destination warehouse ──────────────────
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

        grnRepository.save(grn);

        return "Phiếu GIN " + gin.getGinNumber() + " đã ghi sổ. " + toStatusMsg
                + " Phiếu GRN " + grn.getGrnNumber() + " đã được tạo tự động tại kho đích.";
    }

    private String createBackOrderGINForSO(SalesOrder so, GoodsIssueNote postedGin) {
        String ginNumber = generateGINNumber();
        GoodsIssueNote newGin = new GoodsIssueNote();
        newGin.setGinNumber(ginNumber); newGin.setSalesOrder(so); newGin.setWarehouse(so.getWarehouse());
        newGin.setGiStatus("Draft"); newGin.setDetails(new ArrayList<>());

        for (SalesOrderDetail soDetail : so.getDetails()) {
            if (soDetail.getOrderedQty() - soDetail.getIssuedQty() > 0) {
                GoodsIssueDetail old = postedGin.getDetails().stream().filter(d -> d.getSalesOrderDetail().equals(soDetail)).findFirst().orElse(null);
                GoodsIssueDetail newDetail = new GoodsIssueDetail();
                newDetail.setGoodsIssueNote(newGin); newDetail.setSalesOrderDetail(soDetail); newDetail.setProduct(soDetail.getProduct());
                newDetail.setIssuedQty(0); newDetail.setUom(soDetail.getUom());
                newDetail.setBatchNumber(old != null ? old.getBatchNumber() : ""); newDetail.setBin(old != null ? old.getBin() : null);
                newGin.getDetails().add(newDetail);
            }
        }
        ginRepository.save(newGin);
        return ginNumber;
    }

    private String createBackOrderGINForTO(TransferOrder to, GoodsIssueNote postedGin) {
        String ginNumber = generateGINNumber();
        GoodsIssueNote newGin = new GoodsIssueNote();
        newGin.setGinNumber(ginNumber);
        newGin.setTransferOrder(to);
        newGin.setWarehouse(to.getSourceWarehouse());
        newGin.setGiStatus("Draft");
        newGin.setDetails(new ArrayList<>());

        // DUYỆT TRÊN CHI TIẾT CỦA GIN VỪA POST
        for (GoodsIssueDetail postedDetail : postedGin.getDetails()) {
            // Giả sử trong GIN cũ bạn định xuất 5 (planned), nhưng thực tế chỉ xuất 3 (issued)
            // Bạn cần một field như 'plannedQty' hoặc lấy từ chính số lượng ban đầu của Detail đó
            int planned = postedDetail.getPlannedQty(); // Bạn nên có field này để biết GIN này định xuất bao nhiêu
            int issued = postedDetail.getIssuedQty();
            int missing = planned - issued;

            if (missing > 0) {
                GoodsIssueDetail newDetail = new GoodsIssueDetail();
                newDetail.setGoodsIssueNote(newGin);
                newDetail.setTransferOrderDetail(postedDetail.getTransferOrderDetail());
                newDetail.setProduct(postedDetail.getProduct());
                newDetail.setUom(postedDetail.getUom());

                // QUAN TRỌNG: Giữ nguyên Bin và Batch từ GIN cũ vì hàng thường đã được quy hoạch sẵn
                newDetail.setBatchNumber(postedDetail.getBatchNumber());
                newDetail.setBin(postedDetail.getBin());

                newDetail.setIssuedQty(0);
                newDetail.setPlannedQty(missing); // GIN mới sẽ gánh phần còn lại của GIN cũ

                newGin.getDetails().add(newDetail);
            }
        }

        if (!newGin.getDetails().isEmpty()) {
            ginRepository.save(newGin);
            return ginNumber;
        }
        return null;
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

    private String generateGINNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "GIN-" + dateStr + "-";
        String maxNumber = ginRepository.findMaxGinNumber(prefix);
        return prefix + String.format("%03d", maxNumber != null ? Integer.parseInt(maxNumber.substring(prefix.length())) + 1 : 1);
    }

    private String generateGRNNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "GRN-" + dateStr + "-";
        String maxNumber = grnRepository.findMaxGrnNumber(prefix);
        return prefix + String.format("%03d", maxNumber != null ? Integer.parseInt(maxNumber.substring(prefix.length())) + 1 : 1);
    }

    private BigDecimal getConversionFactor(Product product, String uom) {
        if (uom.equals(product.getBaseUoM())) return BigDecimal.ONE;
        return uomConversionRepository.findByProduct_ProductId(product.getProductId()).stream()
                .filter(c -> c.getFromUoM().equals(uom)).findFirst().map(c -> BigDecimal.valueOf(c.getConversionFactor())).orElse(BigDecimal.ONE);
    }

    private GoodsIssueNoteDTO mapToListDTO(GoodsIssueNote gin) {
        String orderNumber = gin.getSalesOrder() != null ? gin.getSalesOrder().getSoNumber() : gin.getTransferOrder().getToNumber();
        String customerName = gin.getSalesOrder() != null && gin.getSalesOrder().getCustomer() != null ? gin.getSalesOrder().getCustomer().getPartnerName() : (gin.getTransferOrder() != null ? "TO " + gin.getTransferOrder().getDestinationWarehouse().getWarehouseName() : null);
        return GoodsIssueNoteDTO.builder()
                .ginId(gin.getGinId()).ginNumber(gin.getGinNumber()).soNumber(orderNumber).customerName(customerName)
                .warehouseName(gin.getWarehouse() != null ? gin.getWarehouse().getWarehouseName() : null).giStatus(gin.getGiStatus()).build();
    }

    private GoodsIssueNoteDTO mapToFullDTO(GoodsIssueNote gin) {
        GoodsIssueNoteDTO dto = mapToListDTO(gin);
        if (gin.getDetails() != null) dto.setDetails(gin.getDetails().stream().map(this::mapDetailToDTO).collect(Collectors.toList()));
        return dto;
    }

    private GoodsIssueDetailDTO mapDetailToDTO(GoodsIssueDetail detail) {
        String displayName = detail.getProduct() != null ? detail.getProduct().getSku() + " - " + detail.getProduct().getProductName() : "";
        Integer orderedQty = null;
        if (detail.getSalesOrderDetail() != null) orderedQty = detail.getSalesOrderDetail().getOrderedQty() - detail.getSalesOrderDetail().getIssuedQty();
        else if (detail.getTransferOrderDetail() != null) orderedQty = detail.getTransferOrderDetail().getRequestedQty() - detail.getTransferOrderDetail().getIssuedQty();
        
        return GoodsIssueDetailDTO.builder()
                .giDetailId(detail.getGiDetailId()).productDisplayName(displayName).uom(detail.getUom())
                .orderedQty(orderedQty).issuedQty(detail.getIssuedQty()).batchNumber(detail.getBatchNumber())
                .binLocation(detail.getBin() != null ? detail.getBin().getBinLocation() : null).build();
    }

    // ── Private record for GRN destination allocation tracking ───────────────
    private record GRNAllocation(TransferOrderDetail toDetail, Product product,
                                  String batchNumber, Bin destBin, String uom, int issuedQty) {}
}
