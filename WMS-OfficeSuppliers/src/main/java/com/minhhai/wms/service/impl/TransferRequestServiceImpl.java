package com.minhhai.wms.service.impl;

import com.minhhai.wms.dto.TransferDetailDTO;
import com.minhhai.wms.dto.TransferRequestDTO;
import com.minhhai.wms.entity.*;
import com.minhhai.wms.repository.*;
import com.minhhai.wms.service.TransferRequestService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TransferRequestServiceImpl implements TransferRequestService {

    private final TransferRepository transferRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final StockBatchRepository stockBatchRepository;
    private final ProductUoMConversionRepository uomConversionRepository;
    private final StockMovementRepository stockMovementRepository;

    //query
    @Override
    @Transactional(readOnly = true)
    public TransferRequestDTO getTRById(Integer id) {
        Transfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + id));
        return mapToDTO(transfer);
    }
    //danh sach tr minh gui cho kho khac
    @Override
    public List<TransferRequestDTO> getOutgoingTransferRequests(Integer warehouseId, String status, Integer sourceWarehouseId) {
        List<Transfer> transfers;

        boolean hasStatus = status != null && !status.isBlank();
        boolean hasSourceWarehouse = sourceWarehouseId != null;

        if (hasStatus && hasSourceWarehouse) {
            transfers = transferRepository.findBySourceWarehouse_WarehouseIdAndTransferStatusAndDestinationWarehouse_WarehouseId(sourceWarehouseId, status, warehouseId);
        } else if (hasStatus) {
            transfers = transferRepository.findByDestinationWarehouse_WarehouseIdAndTransferStatus(warehouseId, status);
        } else if (hasSourceWarehouse) {
            transfers = transferRepository.findBySourceWarehouse_WarehouseIdAndDestinationWarehouse_WarehouseId(sourceWarehouseId, warehouseId);
        } else {
            transfers = transferRepository.findByDestinationWarehouse_WarehouseId(warehouseId);
        }

        return transfers.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    //danh sach minh duyet cho kho khac gui toi
    @Override
    public List<TransferRequestDTO> getIncomingWarehouseTransferRequests(Integer warehouseId, String status, Integer destWarehouseId) {
        List<Transfer> transfers;

        boolean hasStatus = status != null && !status.isBlank();
        boolean hasDestWarehouse = destWarehouseId != null;

        if (hasStatus && hasDestWarehouse) {
            transfers = transferRepository.findBySourceWarehouse_WarehouseIdAndTransferStatusAndDestinationWarehouse_WarehouseId(warehouseId, status, destWarehouseId);
        } else if (hasStatus) {
            transfers = transferRepository.findBySourceWarehouse_WarehouseIdAndTransferStatus(warehouseId, status);
        } else if (hasDestWarehouse) {
            transfers = transferRepository.findBySourceWarehouse_WarehouseIdAndDestinationWarehouse_WarehouseId(warehouseId, destWarehouseId);
        } else {
            transfers = transferRepository.findBySourceWarehouse_WarehouseId(warehouseId);
        }

        return transfers.stream().map(this::mapToDTO).collect(Collectors.toList());
    }
    //approve - reject tr
    @Override
    public void approveTR(Integer id, User user) {

        Transfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + id));

        if (!"Pending".equals(transfer.getTransferStatus())) {
            throw new IllegalArgumentException("Only Pending transfers can be approved.");
        }

        if (!user.getWarehouse().getWarehouseId()
                .equals(transfer.getSourceWarehouse().getWarehouseId())) {
            throw new IllegalArgumentException("Only source warehouse can approve this transfer.");
        }

        Warehouse sourceWarehouse = transfer.getSourceWarehouse();

        for (TransferDetail detail : transfer.getDetails()) {

            Product product = detail.getProduct();

            // 🔥 1️⃣ Convert về Base UOM
            BigDecimal conversionFactor = getConversionFactor(product, detail.getUom());

            int baseQtyNeeded = BigDecimal.valueOf(detail.getQuantity())
                    .multiply(conversionFactor)
                    .intValue();

            int remaining = baseQtyNeeded;

            // 🔥 2️⃣ FIFO lấy batch
            List<StockBatch> batches =
                    stockBatchRepository
                            .findByWarehouse_WarehouseIdAndProduct_ProductIdOrderByArrivalDateTimeAsc(
                                    sourceWarehouse.getWarehouseId(),
                                    product.getProductId());

            for (StockBatch batch : batches) {

                if (remaining <= 0) break;

                int freeQty = batch.getQtyAvailable() - batch.getQtyReserved();
                if (freeQty <= 0) continue;

                int reserveQty = Math.min(freeQty, remaining);

                batch.setQtyReserved(batch.getQtyReserved() + reserveQty);
                stockBatchRepository.save(batch);

                // Log movement theo BASE UOM
                StockMovement movement = StockMovement.builder()
                        .warehouse(sourceWarehouse)
                        .product(product)
                        .bin(batch.getBin())
                        .batchNumber(batch.getBatchNumber())
                        .movementType("Reserve")
                        .stockType("Reserved")
                        .quantity(reserveQty) // luôn là base UOM
                        .uom(product.getBaseUoM())
                        .balanceAfter(batch.getQtyReserved())
                        .build();

                stockMovementRepository.save(movement);
                remaining -= reserveQty;
            }

            if (remaining > 0) {
                throw new IllegalArgumentException(
                        "Not enough stock for product "
                                + product.getProductName()
                                + ". Missing "
                                + remaining + " " + product.getBaseUoM());
            }
        }

        transfer.setTransferStatus("Approved");
        transferRepository.save(transfer);
    }

    @Override
    public void rejectTR(Integer id, String reason) {
        Transfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + id));

        if (!"Pending".equals(transfer.getTransferStatus())) {
            throw new IllegalArgumentException("Only Transfers with 'Pending' status can be rejected.");
        }

        transfer.setTransferStatus("Rejected");
        transfer.setRejectReason(reason);
        transferRepository.save(transfer);
    }

    //save draft, submit, delete tr
    @Override
    public TransferRequestDTO submitTR(TransferRequestDTO trDTO, User user) {
        // ================= VALIDATE DETAIL =================
        List<TransferDetailDTO> allDetails = trDTO.getDetails();

        if (allDetails == null || allDetails.isEmpty()) {
            throw new IllegalArgumentException("At least one product line is required to submit.");
        }

        for (int i = 0; i < allDetails.size(); i++) {
            TransferDetailDTO d = allDetails.get(i);

            if (d.getProductId() == null) {
                throw new IllegalArgumentException("Product is required on line " + (i + 1) + ".");
            }
            if (d.getUom() == null || d.getUom().isBlank()) {
                throw new IllegalArgumentException("UoM is required on line " + (i + 1) + ".");
            }
            if (d.getTransferQty() == null || d.getTransferQty() <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than 0 on line " + (i + 1) + ".");
            }
        }

        // ================= MAP ENTITY =================
        Transfer transfer = mapToEntity(trDTO, user);

        // ================= UPDATE STATUS =================
        transfer.setTransferStatus("Pending");
        transfer = transferRepository.save(transfer);

        return mapToDTO(transfer);
    }

    @Override
    public void saveDraftTR(TransferRequestDTO trDTO, User user) {
        Transfer tr = mapToEntity(trDTO, user);
        tr.setTransferStatus("Draft");
        tr = transferRepository.save(tr);
        mapToDTO(tr);
    }

    @Override
    public void deleteTR(Integer id, User user) {
        Transfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + id));
        if (!"Draft".equals(transfer.getTransferStatus())) {
            throw new IllegalArgumentException("Only Draft Transfers can be deleted.");
        }

        transferRepository.delete(transfer);
    }

    //map entity -> dto
    private TransferRequestDTO mapToDTO(Transfer transfer) {
        TransferRequestDTO dto = TransferRequestDTO.builder()
                .trId(transfer.getTransferId())
                .transferNumber(transfer.getTransferNumber())
                .sourceWarehouseId(transfer.getSourceWarehouse().getWarehouseId())
                .sourceWarehouseName(transfer.getSourceWarehouse().getWarehouseName())
                .destinationWarehouseId(transfer.getDestinationWarehouse().getWarehouseId())
                .destinationWarehouseName(transfer.getDestinationWarehouse().getWarehouseName())
                .trStatus(transfer.getTransferStatus())
                .rejectReason(transfer.getRejectReason())
                .build();

        if(transfer.getDetails() != null){
            List<TransferDetailDTO> details = transfer.getDetails().stream()
                    .map(this::mapDetailToDTO)
                    .collect(Collectors.toList());
            dto.setDetails(details);
        }
        return dto;
    }
    private TransferDetailDTO mapDetailToDTO(TransferDetail detail) {
        String displayName = "";
        if (detail.getProduct() != null) {
            displayName = detail.getProduct().getSku() + " - " + detail.getProduct().getProductName();
        }
        return TransferDetailDTO.builder()
                .transferDetailId(detail.getTDetailId())
                .productId(detail.getProduct() != null ? detail.getProduct().getProductId() : null)
                .productDisplayName(displayName)
                .uom(detail.getUom())
                .transferQty(detail.getQuantity())
                .build();
    }

    //map dto -> entity
    private Transfer mapToEntity(TransferRequestDTO dto, User currentUser) {
        Transfer transfer;
        if (dto.getTrId() != null) {
            // Update existing Transfer
            transfer = transferRepository.findById(dto.getTrId())
                    .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + dto.getTrId()));
            // Only allow editing Draft or Rejected TRs
            if (!"Draft".equals(transfer.getTransferStatus()) && !"Rejected".equals(transfer.getTransferStatus())) {
                throw new IllegalArgumentException("Only Draft or Rejected Transfers can be edited.");
            }
            // Clear old details
            if (transfer.getDetails() != null) {
                transfer.getDetails().clear();
            }
        } else {
            transfer = new Transfer();

            // CHỈ generate nếu DTO chưa có number
            if (dto.getTransferNumber() == null || dto.getTransferNumber().isBlank()) {
                transfer.setTransferNumber(generateTransferNumber());
            } else {
                transfer.setTransferNumber(dto.getTransferNumber());
            }

            transfer.setDestinationWarehouse(currentUser.getWarehouse());
            transfer.setDetails(new ArrayList<>());
        }

        // Set source warehouse
        if (dto.getSourceWarehouseId() != null) {
            Warehouse sourceWarehouse = warehouseRepository.findById(dto.getSourceWarehouseId())
                    .orElseThrow(() -> new IllegalArgumentException("Source Warehouse not found: " + dto.getSourceWarehouseId()));
            transfer.setSourceWarehouse(sourceWarehouse);
        }

        // Map detail lines (filter empty ones)
        List<TransferDetailDTO> validDetails = filterEmptyDetails(dto.getDetails());
        for (TransferDetailDTO detailDTO : validDetails) {
            TransferDetail detail = new TransferDetail();
            detail.setTransfer(transfer);


            Product product = productRepository.findById(detailDTO.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + detailDTO.getProductId()));
            detail.setProduct(product);
            detail.setUom(detailDTO.getUom());
            detail.setQuantity(detailDTO.getTransferQty());
            transfer.getDetails().add(detail);
        }

        return transfer;
    }
    //uom conversion
    private BigDecimal getConversionFactor(Product product, String uom) {

        if (uom.equals(product.getBaseUoM())) {
            return BigDecimal.ONE;
        }

        return uomConversionRepository
                .findByProduct_ProductId(product.getProductId())
                .stream()
                .filter(conv -> conv.getFromUoM().equals(uom))
                .map(conv -> BigDecimal.valueOf(conv.getConversionFactor()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("No UOM conversion found for "
                                + uom + " of product " + product.getProductName()));
    }
    //helper
    private List<TransferDetailDTO> filterEmptyDetails(@Valid @NotEmpty(message = "Đơn hàng phải có ít nhất một dòng sản phẩm") List<TransferDetailDTO> details) {
        if (details == null) return new ArrayList<>();
        return details.stream()
                .filter(d -> d.getProductId() != null)
                .collect(Collectors.toList());
    }

    private String generateTransferNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "TRS-" + dateStr + "-";
        String maxNumber = transferRepository.findMaxTransferNumber(prefix);

        int nextNum = 1;
        if (maxNumber != null) {
            // Lấy 3 số cuối của mã lớn nhất và cộng thêm 1
            String suffix = maxNumber.substring(prefix.length());
            nextNum = Integer.parseInt(suffix) + 1;
        }

        return prefix + String.format("%03d", nextNum);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, String>> getAvailableUoMs(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        List<Map<String, String>> uomList = new ArrayList<>();
        Map<String, String> baseEntry = new LinkedHashMap<>();
        baseEntry.put("uom", product.getBaseUoM());
        baseEntry.put("label", product.getBaseUoM() + " (Base)");
        uomList.add(baseEntry);
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
}
