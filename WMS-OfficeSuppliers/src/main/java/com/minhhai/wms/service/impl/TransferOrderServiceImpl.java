package com.minhhai.wms.service.impl;

import com.minhhai.wms.dto.TransferOrderDetailDTO;
import com.minhhai.wms.dto.TransferOrderDTO;
import com.minhhai.wms.entity.*;
import com.minhhai.wms.repository.*;
import com.minhhai.wms.service.TransferOrderService;
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
public class TransferOrderServiceImpl implements TransferOrderService {

    private final TransferOrderRepository transferOrderRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final StockBatchRepository stockBatchRepository;
    private final ProductUoMConversionRepository uomConversionRepository;
    private final StockMovementRepository stockMovementRepository;

    @Override
    @Transactional(readOnly = true)
    public TransferOrderDTO getTOById(Integer id) {
        TransferOrder transfer = transferOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TransferOrder not found: " + id));
        return mapToDTO(transfer);
    }

    @Override
    public List<TransferOrderDTO> getOutgoingTransferOrders(Integer warehouseId, String status, Integer sourceWarehouseId) {
        List<TransferOrder> transfers;
        boolean hasStatus = status != null && !status.isBlank();
        boolean hasSourceWarehouse = sourceWarehouseId != null;

        if (hasStatus && hasSourceWarehouse) {
            transfers = transferOrderRepository.findBySourceWarehouse_WarehouseIdAndStatusAndDestinationWarehouse_WarehouseId(sourceWarehouseId, status, warehouseId);
        } else if (hasStatus) {
            transfers = transferOrderRepository.findByDestinationWarehouse_WarehouseIdAndStatus(warehouseId, status);
        } else if (hasSourceWarehouse) {
            transfers = transferOrderRepository.findBySourceWarehouse_WarehouseIdAndDestinationWarehouse_WarehouseId(sourceWarehouseId, warehouseId);
        } else {
            transfers = transferOrderRepository.findByDestinationWarehouse_WarehouseId(warehouseId);
        }
        return transfers.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public List<TransferOrderDTO> getIncomingWarehouseTransferOrders(Integer warehouseId, String status, Integer destWarehouseId) {
        List<TransferOrder> transfers;
        boolean hasStatus = status != null && !status.isBlank();
        boolean hasDestWarehouse = destWarehouseId != null;

        if (hasStatus && hasDestWarehouse) {
            transfers = transferOrderRepository.findBySourceWarehouse_WarehouseIdAndStatusAndDestinationWarehouse_WarehouseId(warehouseId, status, destWarehouseId);
        } else if (hasStatus) {
            transfers = transferOrderRepository.findBySourceWarehouse_WarehouseIdAndStatus(warehouseId, status);
        } else if (hasDestWarehouse) {
            transfers = transferOrderRepository.findBySourceWarehouse_WarehouseIdAndDestinationWarehouse_WarehouseId(warehouseId, destWarehouseId);
        } else {
            transfers = transferOrderRepository.findBySourceWarehouse_WarehouseId(warehouseId);
        }
        return transfers.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public void approveTO(Integer id, User user) {
        TransferOrder transfer = transferOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TransferOrder not found: " + id));

        if (!"Pending".equals(transfer.getStatus())) {
            throw new IllegalArgumentException("Only Pending transfers can be approved.");
        }

        if (!user.getWarehouse().getWarehouseId()
                .equals(transfer.getSourceWarehouse().getWarehouseId())) {
            throw new IllegalArgumentException("Only source warehouse can approve this transfer.");
        }

        Warehouse sourceWarehouse = transfer.getSourceWarehouse();

        for (TransferOrderDetail detail : transfer.getDetails()) {
            Product product = detail.getProduct();
            BigDecimal conversionFactor = getConversionFactor(product, detail.getUom());
            int baseQtyNeeded = BigDecimal.valueOf(detail.getRequestedQty())
                    .multiply(conversionFactor)
                    .intValue();
            int remaining = baseQtyNeeded;

            List<StockBatch> batches = stockBatchRepository
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

                StockMovement movement = StockMovement.builder()
                        .warehouse(sourceWarehouse)
                        .product(product)
                        .bin(batch.getBin())
                        .batchNumber(batch.getBatchNumber())
                        .movementType("Reserve")
                        .stockType("Reserved")
                        .quantity(reserveQty)
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

        transfer.setStatus("Approved");
        transferOrderRepository.save(transfer);
    }

    @Override
    public void rejectTO(Integer id, String reason) {
        TransferOrder transfer = transferOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TransferOrder not found: " + id));

        if (!"Pending".equals(transfer.getStatus())) {
            throw new IllegalArgumentException("Only Transfers with 'Pending' status can be rejected.");
        }

        transfer.setStatus("Rejected");
        transfer.setRejectReason(reason);
        transferOrderRepository.save(transfer);
    }

    @Override
    public TransferOrderDTO submitTO(TransferOrderDTO toDTO, User user) {
        List<TransferOrderDetailDTO> allDetails = toDTO.getDetails();

        if (allDetails == null || allDetails.isEmpty()) {
            throw new IllegalArgumentException("At least one product line is required to submit.");
        }

        for (int i = 0; i < allDetails.size(); i++) {
            TransferOrderDetailDTO d = allDetails.get(i);

            if (d.getProductId() == null) {
                throw new IllegalArgumentException("Product is required on line " + (i + 1) + ".");
            }
            if (d.getUom() == null || d.getUom().isBlank()) {
                throw new IllegalArgumentException("UoM is required on line " + (i + 1) + ".");
            }
            if (d.getRequestedQty() == null || d.getRequestedQty() <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than 0 on line " + (i + 1) + ".");
            }
        }

        TransferOrder transfer = mapToEntity(toDTO, user);
        transfer.setStatus("Pending");
        transfer = transferOrderRepository.save(transfer);

        return mapToDTO(transfer);
    }

    @Override
    public void saveDraftTO(TransferOrderDTO toDTO, User user) {
        TransferOrder tr = mapToEntity(toDTO, user);
        tr.setStatus("Draft");
        tr = transferOrderRepository.save(tr);
        mapToDTO(tr);
    }

    @Override
    public void deleteTO(Integer id, User user) {
        TransferOrder transfer = transferOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TransferOrder not found: " + id));
        if (!"Draft".equals(transfer.getStatus())) {
            throw new IllegalArgumentException("Only Draft Transfers can be deleted.");
        }
        transferOrderRepository.delete(transfer);
    }

    private TransferOrderDTO mapToDTO(TransferOrder transfer) {
        TransferOrderDTO dto = TransferOrderDTO.builder()
                .toId(transfer.getToId())
                .toNumber(transfer.getToNumber())
                .sourceWarehouseId(transfer.getSourceWarehouse().getWarehouseId())
                .sourceWarehouseName(transfer.getSourceWarehouse().getWarehouseName())
                .destinationWarehouseId(transfer.getDestinationWarehouse().getWarehouseId())
                .destinationWarehouseName(transfer.getDestinationWarehouse().getWarehouseName())
                .status(transfer.getStatus())
                .rejectReason(transfer.getRejectReason())
                .build();

        if(transfer.getDetails() != null){
            List<TransferOrderDetailDTO> details = transfer.getDetails().stream()
                    .map(this::mapDetailToDTO)
                    .collect(Collectors.toList());
            dto.setDetails(details);
        }
        return dto;
    }

    private TransferOrderDetailDTO mapDetailToDTO(TransferOrderDetail detail) {
        String displayName = "";
        if (detail.getProduct() != null) {
            displayName = detail.getProduct().getSku() + " - " + detail.getProduct().getProductName();
        }
        return TransferOrderDetailDTO.builder()
                .toDetailId(detail.getToDetailId())
                .productId(detail.getProduct() != null ? detail.getProduct().getProductId() : null)
                .productDisplayName(displayName)
                .uom(detail.getUom())
                .requestedQty(detail.getRequestedQty())
                .issuedQty(detail.getIssuedQty())
                .receivedQty(detail.getReceivedQty())
                .build();
    }

    private TransferOrder mapToEntity(TransferOrderDTO dto, User currentUser) {
        TransferOrder transfer;
        if (dto.getToId() != null) {
            transfer = transferOrderRepository.findById(dto.getToId())
                    .orElseThrow(() -> new IllegalArgumentException("TransferOrder not found: " + dto.getToId()));
            if (!"Draft".equals(transfer.getStatus()) && !"Rejected".equals(transfer.getStatus())) {
                throw new IllegalArgumentException("Only Draft or Rejected Transfers can be edited.");
            }
            if (transfer.getDetails() != null) {
                transfer.getDetails().clear();
            }
        } else {
            transfer = new TransferOrder();
            if (dto.getToNumber() == null || dto.getToNumber().isBlank()) {
                transfer.setToNumber(generateToNumber());
            } else {
                transfer.setToNumber(dto.getToNumber());
            }
            transfer.setDestinationWarehouse(currentUser.getWarehouse());
            transfer.setCreatedBy(currentUser);
            transfer.setDetails(new ArrayList<>());
        }

        if (dto.getSourceWarehouseId() != null) {
            Warehouse sourceWarehouse = warehouseRepository.findById(dto.getSourceWarehouseId())
                    .orElseThrow(() -> new IllegalArgumentException("Source Warehouse not found: " + dto.getSourceWarehouseId()));
            transfer.setSourceWarehouse(sourceWarehouse);
        }

        List<TransferOrderDetailDTO> validDetails = filterEmptyDetails(dto.getDetails());
        for (TransferOrderDetailDTO detailDTO : validDetails) {
            TransferOrderDetail detail = new TransferOrderDetail();
            detail.setTransferOrder(transfer);

            Product product = productRepository.findById(detailDTO.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + detailDTO.getProductId()));
            detail.setProduct(product);
            detail.setUom(detailDTO.getUom());
            detail.setRequestedQty(detailDTO.getRequestedQty());
            transfer.getDetails().add(detail);
        }

        return transfer;
    }

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

    private List<TransferOrderDetailDTO> filterEmptyDetails(@Valid @NotEmpty(message = "Đơn hàng phải có ít nhất một dòng sản phẩm") List<TransferOrderDetailDTO> details) {
        if (details == null) return new ArrayList<>();
        return details.stream()
                .filter(d -> d.getProductId() != null)
                .collect(Collectors.toList());
    }

    private String generateToNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "TRO-" + dateStr + "-";
        String maxNumber = transferOrderRepository.findMaxToNumber(prefix);

        int nextNum = 1;
        if (maxNumber != null) {
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