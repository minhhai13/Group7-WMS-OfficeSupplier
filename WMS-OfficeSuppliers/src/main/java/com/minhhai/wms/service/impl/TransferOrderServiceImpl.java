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
    private final ProductUoMConversionRepository uomConversionRepository;
    private final GoodsIssueNoteRepository ginRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final StockBatchRepository stockBatchRepository;
    private final StockMovementRepository stockMovementRepository;

    //list dto
    @Override
    public List<TransferOrderDTO> getOutgoingWHTransferOrders(Integer warehouseId, Integer sourceWarehouseId, String status) {
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
    public List<TransferOrderDTO> getIncomingWHTransferOrders(Integer destWarehouseId, Integer warehouseId, String status) {
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
    public TransferOrderDTO getTRById(Integer id) {
        TransferOrder transfer = transferOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TransferOrder not found: " + id));
        return mapToDTO(transfer);
    }

    //Transfer Request: 'Draft', 'Pending', 'Rejected'
    //Transfer Order: 'Approved', 'In-Transit', 'Completed'
    //save + submit + delete TR
    @Override
    public void saveDraftTR(TransferOrderDTO trDTO, User user) {
        TransferOrder tr = mapToEntity(trDTO, user);
        tr.setStatus("Draft");
        tr = transferOrderRepository.save(tr);
        mapToDTO(tr);
    }

    @Override
    public TransferOrderDTO submitTR(TransferOrderDTO trDTO, User user) {

        //trước khi submit cần validate:
        //check if user's warehouse matches destination warehouse of the TR
        if (!user.getWarehouse().getWarehouseId().equals(trDTO.getDestinationWarehouseId())) {
            throw new IllegalArgumentException("You can only submit transfer requests if you're from Dest Warehouse.");
        }

        //b1: check if tr has at least 1 detail
        List<TransferOrderDetailDTO> allDetails = trDTO.getDetails();
        if (allDetails == null || allDetails.isEmpty()) {
            throw new IllegalArgumentException("At least one product line is required to submit.");
        }
        //b2: validate details
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
        //b3: tr.setStatus("Pending");
        TransferOrder transfer = mapToEntity(trDTO, user);
        transfer.setStatus("Pending");
        //b4: save
        transfer = transferOrderRepository.save(transfer);
        return mapToDTO(transfer);
    }

    @Override
    public void deleteTR(Integer id, User user) {
        TransferOrder transfer = transferOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TransferOrder not found: " + id));
        if (!"Draft".equals(transfer.getStatus())) {
            throw new IllegalArgumentException("Only Draft Transfers can be deleted.");
        }
        if (!user.getWarehouse().getWarehouseId()
                .equals(transfer.getDestinationWarehouse().getWarehouseId())) {
            throw new IllegalArgumentException("You can only delete transfer requests if you're from Dest Warehouse.");
        }
        transferOrderRepository.delete(transfer);
    }
    //approved + reject tr
    @Override
    public void approveTR(Integer id, User user) {
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

        // Track per-batch allocations to build GIN detail lines
        List<GINAllocation> ginAllocations = new ArrayList<>();

        for (TransferOrderDetail detail : transfer.getDetails()) {
            Product product = detail.getProduct();
            BigDecimal conversionFactor = getConversionFactor(product, detail.getUom());
            int baseQtyNeeded = BigDecimal.valueOf(detail.getRequestedQty())
                    .multiply(conversionFactor)
                    .intValueExact();
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
                ginAllocations.add(new GINAllocation(detail, product, batch, reserveQty, detail.getUom()));
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

        // ── Auto-create Draft GIN for source warehouse ──────────────────────
        GoodsIssueNote gin = new GoodsIssueNote();
        gin.setGinNumber(generateGINNumber());
        gin.setTransferOrder(transfer);
        gin.setWarehouse(sourceWarehouse);
        gin.setGiStatus("Draft");
        gin.setDetails(new ArrayList<>());

        for (GINAllocation alloc : ginAllocations) {
            GoodsIssueDetail ginDetail = new GoodsIssueDetail();
            ginDetail.setGoodsIssueNote(gin);
            ginDetail.setTransferOrderDetail(alloc.toDetail());
            ginDetail.setProduct(alloc.product());
            ginDetail.setIssuedQty(0);
            ginDetail.setUom(alloc.uom());
            ginDetail.setBatchNumber(alloc.batch().getBatchNumber());
            ginDetail.setBin(alloc.batch().getBin());
            ginDetail.setPlannedQty(alloc.reservedQty());
            gin.getDetails().add(ginDetail);
        }

        ginRepository.save(gin);
    }

    @Override
    public void rejectTR(Integer id, User user, String reason) {
        TransferOrder transfer = transferOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TransferOrder not found: " + id));

        if (!"Pending".equals(transfer.getStatus())) {
            throw new IllegalArgumentException("Only Transfers with 'Pending' status can be rejected.");
        }
        if (!user.getWarehouse().getWarehouseId()
                .equals(transfer.getSourceWarehouse().getWarehouseId())) {
            throw new IllegalArgumentException("Only source warehouse can reject this transfer.");
        }

        transfer.setStatus("Rejected");
        transfer.setRejectReason(reason);
        transferOrderRepository.save(transfer);
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
    //helper
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

    private String generateGINNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "GIN-" + dateStr + "-";
        String maxNumber = ginRepository.findMaxGinNumber(prefix);
        int nextNum = 1;
        if (maxNumber != null) {
            String suffix = maxNumber.substring(prefix.length());
            nextNum = Integer.parseInt(suffix) + 1;
        }
        return prefix + String.format("%03d", nextNum);
    }

    // private record for GIN batch allocation tracking
    private record GINAllocation(TransferOrderDetail toDetail, Product product, StockBatch batch, int reservedQty, String uom) {}
}