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
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoodsReceiptNoteServiceImpl implements GoodsReceiptNoteService {

    private final GoodsReceiptNoteRepository grnRepository;
    private final GoodsReceiptDetailRepository grDetailRepository;
    private final PurchaseOrderDetailRepository poDetailRepository;
    private final PurchaseOrderRepository poRepository;
    private final BinRepository binRepository;
    private final StockBatchRepository stockBatchRepository;
    private final StockMovementRepository stockMovementRepository;

    @Override
    public List<GoodsReceiptNote> getGRNsByWarehouse(Integer warehouseId, String status) {
        if (status != null && !status.isEmpty()) {
            return grnRepository.findByWarehouse_WarehouseIdAndGrStatusOrderByGrnIdDesc(warehouseId, status);
        }
        return grnRepository.findByWarehouse_WarehouseIdOrderByGrnIdDesc(warehouseId);
    }

    @Override
    public GoodsReceiptNote getGRNById(Integer grnId) {
        return grnRepository.findById(grnId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid GRN ID"));
    }

    @Override
    @Transactional
    public void processGRN(GoodsReceiptNoteDTO dto, Integer warehouseId) {
        GoodsReceiptNote grn = getGRNById(dto.getGrnId());
        
        if (!grn.getWarehouse().getWarehouseId().equals(warehouseId)) {
            throw new SecurityException("Unauthorized");
        }
        
        if ("Posted".equals(grn.getGrStatus())) {
            throw new IllegalStateException("GRN is already posted");
        }

        PurchaseOrder po = grn.getPurchaseOrder();

        for (GoodsReceiptDetailDTO detailDTO : dto.getDetails()) {
            if (detailDTO.getReceivedQty() == null || detailDTO.getReceivedQty() <= 0) {
                continue; // Skip lines that haven't received anything new in this entry
            }

            PurchaseOrderDetail poDetail = poDetailRepository.findById(detailDTO.getPoDetailId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid PO Detail"));

            if (detailDTO.getReceivedQty() > (poDetail.getOrderedQty() - poDetail.getReceivedQty())) {
                throw new IllegalArgumentException("Cannot receive more than ordered for product: " + poDetail.getProduct().getSku());
            }

            Bin bin = binRepository.findById(detailDTO.getBinId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Bin"));

            // Check if Bin has enough weight capacity
            BigDecimal addedWeight = poDetail.getProduct().getUnitWeight().multiply(new BigDecimal(detailDTO.getReceivedQty()));
            // Here we need to calculate current weight in bin from all batches
            BigDecimal currentWeightInBin = calculateCurrentWeightInBin(bin.getBinId());
            if (currentWeightInBin.add(addedWeight).compareTo(bin.getMaxWeight()) > 0) {
                throw new IllegalArgumentException("Bin overloaded: " + bin.getBinLocation());
            }

            String batchNumber = detailDTO.getBatchNumber();
            if (batchNumber == null || batchNumber.isBlank()) {
                batchNumber = "BATCH-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + poDetail.getPoDetailId();
            }

            // Create GoodsReceiptDetail record
            GoodsReceiptDetail grDetail = new GoodsReceiptDetail();
            grDetail.setGoodsReceiptNote(grn);
            grDetail.setPurchaseOrderDetail(poDetail);
            grDetail.setProduct(poDetail.getProduct());
            grDetail.setReceivedQty(detailDTO.getReceivedQty());
            grDetail.setUom(poDetail.getUom());
            grDetail.setBatchNumber(batchNumber);
            grDetail.setBin(bin);
            grDetailRepository.save(grDetail);

            // Create StockBatch
            StockBatch batch = new StockBatch();
            batch.setWarehouse(grn.getWarehouse());
            batch.setProduct(poDetail.getProduct());
            batch.setBin(bin);
            batch.setBatchNumber(batchNumber);
            batch.setArrivalDateTime(LocalDateTime.now());
            // TODO: In reality, we must convert receivedQty (in order UoM) to BaseUoM for StockBatch, but for phase 1 we assume it's same, 
            // wait, we should do exact conversion
            int baseQty = convertToBaseUoM(poDetail.getProduct(), detailDTO.getReceivedQty(), poDetail.getUom());
            batch.setQtyAvailable(baseQty);
            batch.setUom(poDetail.getProduct().getBaseUoM());
            stockBatchRepository.save(batch);

            // Log StockMovement
            StockMovement movement = new StockMovement();
            movement.setWarehouse(grn.getWarehouse());
            movement.setProduct(poDetail.getProduct());
            movement.setBin(bin);
            movement.setBatchNumber(batchNumber);
            movement.setMovementType("Receipt");
            movement.setStockType("Physical");
            movement.setQuantity(baseQty);
            movement.setUom(poDetail.getProduct().getBaseUoM());
            movement.setBalanceAfter(baseQty); // For new batch, balance after is the qty itself. Real cumulative balance might be tricky.
            stockMovementRepository.save(movement);

            // Update PO Detail total received
            poDetail.setReceivedQty(poDetail.getReceivedQty() + detailDTO.getReceivedQty());
            poDetailRepository.save(poDetail);
        }

        grn.setGrStatus("Posted");
        grnRepository.save(grn);

        // Update PO Status (Complete/Incomplete)
        boolean allFinished = po.getDetails().stream()
                .allMatch(d -> d.getReceivedQty().equals(d.getOrderedQty()));
        boolean anyReceived = po.getDetails().stream()
                .anyMatch(d -> d.getReceivedQty() > 0);

        if (allFinished) {
            po.setPoStatus("Completed");
        } else if (anyReceived) {
            po.setPoStatus("Incomplete");
            
            // Generate next GRN for remaining items so storekeeper can continue later
            GoodsReceiptNote nextGrn = new GoodsReceiptNote();
            nextGrn.setGrnNumber(generateGRNNumber());
            nextGrn.setPurchaseOrder(po);
            nextGrn.setWarehouse(po.getWarehouse());
            nextGrn.setGrStatus("Draft");
            grnRepository.save(nextGrn);
        }
        poRepository.save(po);
    }
    
    private BigDecimal calculateCurrentWeightInBin(Integer binId) {
        List<StockBatch> batches = stockBatchRepository.findByBinBinId(binId);
        BigDecimal total = BigDecimal.ZERO;
        for (StockBatch b : batches) {
            // b.QtyAvailable is in BaseUoM, so unitWeight is per BaseUoM
            BigDecimal qty = new BigDecimal(b.getQtyAvailable() + b.getQtyReserved()); // Physical weight calculation
            total = total.add(b.getProduct().getUnitWeight().multiply(qty));
        }
        return total;
    }

    private int convertToBaseUoM(Product product, int quantity, String uom) {
        if (product.getBaseUoM().equals(uom)) {
            return quantity;
        }
        if (product.getUomConversions() != null) {
            for (ProductUoMConversion conv : product.getUomConversions()) {
                if (conv.getFromUoM().equals(uom) && conv.getToUoM().equals(product.getBaseUoM())) {
                    return quantity * conv.getConversionFactor();
                }
            }
        }
        throw new IllegalArgumentException("No conversion found for " + uom + " to " + product.getBaseUoM());
    }
    
    private String generateGRNNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "GRN-" + dateStr + "-";
        long count = grnRepository.count();
        return prefix + String.format("%03d", count + 1);
    }
}
