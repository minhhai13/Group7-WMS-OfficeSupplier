package com.minhhai.wms.service.impl;

import com.minhhai.wms.dao.*;
import com.minhhai.wms.dto.TransferNoteDTO;
import com.minhhai.wms.dto.TransferNoteDetailDTO;
import com.minhhai.wms.entity.*;
import com.minhhai.wms.service.TransferNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TransferNoteServiceImpl implements TransferNoteService {

    private final TransferNoteDao transferNoteDao;
    private final TransferNoteDetailDao transferNoteDetailDao;
    private final ProductDao productDao;
    private final BinDao binDao;
    private final ProductUoMConversionDao uomConversionDao;
    private final StockBatchDao stockBatchDao;
    private final StockMovementDao stockMovementDao;

    // ==================== Query Methods ====================

    @Override
    @Transactional(readOnly = true)
    public List<TransferNoteDTO> getTransferNotes(Integer warehouseId) {
        List<TransferNote> notes = transferNoteDao.findByWarehouseId(warehouseId);
        // Manual fetch – no Lazy Loading
        for (TransferNote tn : notes) {
            tn.setDetails(transferNoteDetailDao.findByTnId(tn.getTnId()));
        }
        return notes.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TransferNoteDTO getTransferNoteById(Integer tnId) {
        TransferNote tn = transferNoteDao.findById(tnId)
                .orElseThrow(() -> new IllegalArgumentException("TransferNote not found: " + tnId));
        tn.setDetails(transferNoteDetailDao.findByTnId(tnId));
        return mapToDTO(tn);
    }

    // ==================== Create Transfer Note ====================

    @Override
    public void createTransferNote(TransferNoteDTO dto, User currentUser) {
        if (dto.getDetails() == null || dto.getDetails().isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one product line.");
        }

        // Build TN header
        TransferNote tn = new TransferNote();
        tn.setTnNumber(generateTnNumber());
        Warehouse warehouse = new Warehouse();
        warehouse.setWarehouseId(currentUser.getWarehouse().getWarehouseId());
        tn.setWarehouse(warehouse);
        tn.setStatus("Approved");  // Manager tạo xong → Approved, chờ Storekeeper thực hiện
        tn.setCreatedBy(currentUser);
        tn.setCreatedAt(LocalDateTime.now());

        // Save header first to get generated TNID
        transferNoteDao.save(tn);

        Map<Integer, BigDecimal> incomingWeightPerBin = new HashMap<>();

        // Save each detail line
        for (TransferNoteDetailDTO d : dto.getDetails()) {
            if (d.getProductId() == null) continue;

            Product product = productDao.findById(d.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product does not exist: " + d.getProductId()));
            Bin fromBin = binDao.findById(d.getFromBinId())
                    .orElseThrow(() -> new IllegalArgumentException("Source bin does not exist: " + d.getFromBinId()));
            Bin toBin = binDao.findById(d.getToBinId())
                    .orElseThrow(() -> new IllegalArgumentException("Destination bin does not exist: " + d.getToBinId()));

            // Validate destination bin capacity
            int baseQty = BigDecimal.valueOf(d.getQuantity()).multiply(getConversionFactor(product, d.getUom())).intValue();
            BigDecimal incomingWeight = BigDecimal.valueOf(baseQty).multiply(product.getUnitWeight());
            
            BigDecimal currentWeight = stockBatchDao.getTotalWeightByBinId(toBin.getBinId());
            BigDecimal previouslyAccumulated = incomingWeightPerBin.getOrDefault(toBin.getBinId(), BigDecimal.ZERO);
            BigDecimal totalExpectedWeight = currentWeight.add(previouslyAccumulated).add(incomingWeight);

            if (totalExpectedWeight.compareTo(toBin.getMaxWeight()) > 0) {
                BigDecimal remainingCapacity = toBin.getMaxWeight().subtract(currentWeight).subtract(previouslyAccumulated);
                throw new IllegalArgumentException(String.format(
                    "Insufficient capacity in destination bin '%s' for product '%s'. Required: %.2f kg, Remaining Capacity: %.2f kg.",
                    toBin.getBinLocation(), product.getProductName(), incomingWeight, remainingCapacity));
            }
            incomingWeightPerBin.put(toBin.getBinId(), previouslyAccumulated.add(incomingWeight));

            TransferNoteDetail detail = new TransferNoteDetail();
            detail.setTransferNote(tn);
            detail.setProduct(product);
            detail.setBatchNumber(d.getBatchNumber());
            detail.setFromBin(fromBin);
            detail.setToBin(toBin);
            detail.setQuantity(d.getQuantity());
            detail.setUom(d.getUom());

            transferNoteDetailDao.save(detail);
        }
    }

    // ==================== Complete Transfer Note ====================

    @Override
    @Transactional
    public void completeTransferNote(Integer tnId, User storekeeper) {
        TransferNote tn = transferNoteDao.findById(tnId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer note not found."));

        // Guard: same warehouse as storekeeper
        if (!tn.getWarehouse().getWarehouseId().equals(storekeeper.getWarehouse().getWarehouseId())) {
            throw new IllegalArgumentException("You don't have permission to confirm transfer notes for another warehouse.");
        }
        if (!"Approved".equals(tn.getStatus())) {
            throw new IllegalArgumentException("This note is already completed or cancelled.");
        }

        // Manual fetch details (no Lazy Loading)
        tn.setDetails(transferNoteDetailDao.findByTnId(tnId));

        Warehouse warehouse = tn.getWarehouse();

        for (TransferNoteDetail detail : tn.getDetails()) {
            Product product = detail.getProduct();
            int baseQty = BigDecimal.valueOf(detail.getQuantity())
                    .multiply(getConversionFactor(product, detail.getUom()))
                    .intValue();

            // ── 1. Deduct from source Bin ─────────────────────────────────────
            StockBatch fromBatch = stockBatchDao
                    .findByWarehouseIdAndProductIdAndBinIdAndBatchNumber(
                            warehouse.getWarehouseId(),
                            product.getProductId(),
                            detail.getFromBin().getBinId(),
                            detail.getBatchNumber())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Insufficient stock at source bin for: " + product.getProductName()));

            int availableFree = fromBatch.getQtyAvailable() - fromBatch.getQtyReserved();
            if (availableFree < baseQty) {
                throw new IllegalArgumentException(
                        "Tồn kho thực tế không đủ để thực hiện chuyển cho: " + product.getProductName()
                        + " (Hiện có: " + availableFree + ", Cần: " + baseQty + ")");
            }

            fromBatch.setQtyAvailable(fromBatch.getQtyAvailable() - baseQty);
            stockBatchDao.save(fromBatch);

            // Log Transfer-Out movement
            stockMovementDao.save(StockMovement.builder()
                    .warehouse(warehouse)
                    .product(product)
                    .bin(detail.getFromBin())
                    .batchNumber(detail.getBatchNumber())
                    .movementType("Transfer-Out")
                    .stockType("Physical")
                    .quantity(baseQty)
                    .uom(product.getBaseUoM())
                    .balanceAfter(fromBatch.getQtyAvailable())
                    .build());

            // ── 2. Add to destination Bin ─────────────────────────────────────
            StockBatch toBatch = stockBatchDao
                    .findByWarehouseIdAndProductIdAndBinIdAndBatchNumber(
                            warehouse.getWarehouseId(),
                            product.getProductId(),
                            detail.getToBin().getBinId(),
                            detail.getBatchNumber())
                    .orElseGet(() -> StockBatch.builder()
                            .warehouse(warehouse)
                            .product(product)
                            .bin(detail.getToBin())
                            .batchNumber(detail.getBatchNumber())
                            .arrivalDateTime(fromBatch.getArrivalDateTime())
                            .qtyAvailable(0).qtyReserved(0).qtyInTransit(0)
                            .uom(product.getBaseUoM())
                            .build());

            toBatch.setQtyAvailable(toBatch.getQtyAvailable() + baseQty);
            stockBatchDao.save(toBatch);

            // Log Transfer-In movement
            stockMovementDao.save(StockMovement.builder()
                    .warehouse(warehouse)
                    .product(product)
                    .bin(detail.getToBin())
                    .batchNumber(detail.getBatchNumber())
                    .movementType("Transfer-In")
                    .stockType("Physical")
                    .quantity(baseQty)
                    .uom(product.getBaseUoM())
                    .balanceAfter(toBatch.getQtyAvailable())
                    .build());
        }

        // Mark TN as Completed
        transferNoteDao.updateStatus(tnId, "Completed");
    }

    // ==================== Number Generation ====================

    private String generateTnNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "TN-" + dateStr + "-";
        String maxNumber = transferNoteDao.findMaxTnNumber(prefix);
        int nextNum = (maxNumber != null)
                ? Integer.parseInt(maxNumber.substring(prefix.length())) + 1 : 1;
        return prefix + String.format("%03d", nextNum);
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

    // ==================== Mapping: Entity → DTO ====================

    private TransferNoteDTO mapToDTO(TransferNote tn) {
        List<TransferNoteDetailDTO> detailDTOs = new ArrayList<>();
        if (tn.getDetails() != null) {
            detailDTOs = tn.getDetails().stream()
                    .map(d -> TransferNoteDetailDTO.builder()
                            .tnDetailId(d.getTnDetailId())
                            .productId(d.getProduct().getProductId())
                            .productDisplayName(d.getProduct().getSku() + " - " + d.getProduct().getProductName())
                            .batchNumber(d.getBatchNumber())
                            .fromBinId(d.getFromBin().getBinId())
                            .fromBinLocation(d.getFromBin().getBinLocation())
                            .toBinId(d.getToBin().getBinId())
                            .toBinLocation(d.getToBin().getBinLocation())
                            .quantity(d.getQuantity())
                            .uom(d.getUom())
                            .build())
                    .collect(Collectors.toList());
        }

        return TransferNoteDTO.builder()
                .tnId(tn.getTnId())
                .tnNumber(tn.getTnNumber())
                .warehouseId(tn.getWarehouse().getWarehouseId())
                .warehouseName(tn.getWarehouse().getWarehouseName())
                .status(tn.getStatus())
                .details(detailDTOs)
                .build();
    }
}
