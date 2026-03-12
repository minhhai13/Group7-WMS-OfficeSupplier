package com.minhhai.wms.service.impl;

import com.minhhai.wms.dto.TransferNoteDTO;
import com.minhhai.wms.dto.TransferNoteDetailDTO;
import com.minhhai.wms.entity.*;
import com.minhhai.wms.repository.*;
import com.minhhai.wms.service.TransferNoteService;
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
public class TransferNoteServiceImpl implements TransferNoteService {

    private final TransferNoteRepository transferNoteRepository;
    private final ProductRepository productRepository;
    private final BinRepository binRepository;
    private final ProductUoMConversionRepository uomConversionRepository;
    private final StockBatchRepository stockBatchRepository;
    private final StockMovementRepository stockMovementRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TransferNoteDTO> getTransferNotes(Integer warehouseId) {
        return transferNoteRepository.findByWarehouse_WarehouseId(warehouseId).stream()
                .map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TransferNoteDTO getTransferNoteById(Integer tnId) {
        return mapToDTO(transferNoteRepository.findById(tnId)
                .orElseThrow(() -> new IllegalArgumentException("TransferNote not found: " + tnId)));
    }

    @Override
    public void createTransferNote(TransferNoteDTO dto, User currentUser) {
        if (dto.getDetails() == null || dto.getDetails().isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng phải có ít nhất một dòng sản phẩm.");
        }

        Warehouse warehouse = currentUser.getWarehouse();
        TransferNote tn = TransferNote.builder()
                .tnNumber(generateTnNumber())
                .warehouse(warehouse)
                .status("Approved") // Trạng thái "Đã duyệt", chờ Storekeeper thực hiện
                .createdBy(currentUser)
                .details(new ArrayList<>())
                .build();

        for (TransferNoteDetailDTO d : dto.getDetails()) {
            Product product = productRepository.findById(d.getProductId()).orElseThrow();
            Bin fromBin = binRepository.findById(d.getFromBinId()).orElseThrow();
            Bin toBin = binRepository.findById(d.getToBinId()).orElseThrow();

            tn.getDetails().add(TransferNoteDetail.builder()
                    .transferNote(tn).product(product).batchNumber(d.getBatchNumber())
                    .fromBin(fromBin).toBin(toBin).quantity(d.getQuantity()).uom(d.getUom()).build());
        }
        transferNoteRepository.save(tn);
    }

    @Override
    public void completeTransferNote(Integer tnId, User storekeeper) {
        TransferNote tn = transferNoteRepository.findById(tnId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiếu chuyển kho."));
        if (!tn.getWarehouse().getWarehouseId().equals(storekeeper.getWarehouse().getWarehouseId())) {
            throw new IllegalArgumentException("Bạn không có quyền thực hiện xác nhận cho phiếu của kho khác.");
        }
        if (!"Approved".equals(tn.getStatus())) {
            throw new IllegalArgumentException("Phiếu này đã hoàn thành hoặc bị hủy.");
        }

        Warehouse warehouse = tn.getWarehouse();

        for (TransferNoteDetail detail : tn.getDetails()) {
            Product product = detail.getProduct();
            int baseQty = BigDecimal.valueOf(detail.getQuantity())
                    .multiply(getConversionFactor(product, detail.getUom())).intValue();

            // 1. Trừ kho tại Bin nguồn
            StockBatch fromBatch = stockBatchRepository.findByWarehouse_WarehouseIdAndProduct_ProductIdAndBin_BinIdAndBatchNumber(
                            warehouse.getWarehouseId(), product.getProductId(), detail.getFromBin().getBinId(), detail.getBatchNumber())
                    .orElseThrow(() -> new IllegalArgumentException("Không đủ hàng tại Bin nguồn cho: " + product.getProductName()));

            if (fromBatch.getQtyAvailable() - fromBatch.getQtyReserved() < baseQty) {
                throw new IllegalArgumentException("Tồn kho thực tế không đủ để thực hiện chuyển cho: " + product.getProductName());
            }

            fromBatch.setQtyAvailable(fromBatch.getQtyAvailable() - baseQty);
            stockBatchRepository.save(fromBatch);

            // Movement Out
            stockMovementRepository.save(StockMovement.builder()
                    .warehouse(warehouse).product(product).bin(detail.getFromBin()).batchNumber(detail.getBatchNumber())
                    .movementType("Internal Transfer-Out").stockType("Physical").quantity(baseQty)
                    .uom(product.getBaseUoM()).balanceAfter(fromBatch.getQtyAvailable()).build());

            // 2. Cộng kho tại Bin đích
            StockBatch toBatch = stockBatchRepository.findByWarehouse_WarehouseIdAndProduct_ProductIdAndBin_BinIdAndBatchNumber(
                            warehouse.getWarehouseId(), product.getProductId(), detail.getToBin().getBinId(), detail.getBatchNumber())
                    .orElseGet(() -> StockBatch.builder()
                            .warehouse(warehouse).product(product).bin(detail.getToBin()).batchNumber(detail.getBatchNumber())
                            .arrivalDateTime(fromBatch.getArrivalDateTime()).qtyAvailable(0).qtyReserved(0)
                            .qtyInTransit(0).uom(product.getBaseUoM()).build());

            toBatch.setQtyAvailable(toBatch.getQtyAvailable() + baseQty);
            stockBatchRepository.save(toBatch);

            // Movement In
            stockMovementRepository.save(StockMovement.builder()
                    .warehouse(warehouse).product(product).bin(detail.getToBin()).batchNumber(detail.getBatchNumber())
                    .movementType("Internal Transfer-In").stockType("Physical").quantity(baseQty)
                    .uom(product.getBaseUoM()).balanceAfter(toBatch.getQtyAvailable()).build());
        }

        tn.setStatus("Completed");
        transferNoteRepository.save(tn);
    }

    private String generateTnNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "TN-" + dateStr + "-";
        String maxNumber = transferNoteRepository.findMaxTnNumber(prefix);
        int nextNum = maxNumber != null ? Integer.parseInt(maxNumber.substring(prefix.length())) + 1 : 1;
        return prefix + String.format("%03d", nextNum);
    }

    private BigDecimal getConversionFactor(Product product, String uom) {
        if (uom.equals(product.getBaseUoM())) return BigDecimal.ONE;
        return uomConversionRepository.findByProduct_ProductId(product.getProductId()).stream()
                .filter(c -> c.getFromUoM().equals(uom)).findFirst().map(c -> BigDecimal.valueOf(c.getConversionFactor())).orElse(BigDecimal.ONE);
    }

    private TransferNoteDTO mapToDTO(TransferNote tn) {
        return TransferNoteDTO.builder()
                .tnId(tn.getTnId())
                .tnNumber(tn.getTnNumber())
                .warehouseId(tn.getWarehouse().getWarehouseId())
                .warehouseName(tn.getWarehouse().getWarehouseName())
                .status(tn.getStatus())
                .details(tn.getDetails().stream().map(d -> TransferNoteDetailDTO.builder()
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
                        .build()).collect(Collectors.toList()))
                .build();
    }
}
