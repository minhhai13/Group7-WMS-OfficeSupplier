package com.minhhai.wms.service.impl;

import com.minhhai.wms.dao.*;
import com.minhhai.wms.dto.TransferOrderDetailDTO;
import com.minhhai.wms.dto.TransferOrderDTO;
import com.minhhai.wms.entity.*;
import com.minhhai.wms.service.TransferOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TransferOrderServiceImpl implements TransferOrderService {

    private final TransferOrderDao transferOrderDao;
    private final TransferOrderDetailDao toDetailDao;
    private final GoodsIssueNoteDao ginDao;
    private final GoodsIssueDetailDao ginDetailDao;
    private final WarehouseDao warehouseDao;
    private final ProductDao productDao;
    private final StockBatchDao stockBatchDao;
    private final StockMovementDao stockMovementDao;
    private final ProductUoMConversionDao uomConversionDao;

    // ==================== Queries ====================

    /** Outgoing view: dest-warehouse manager sees requests aimed at their WH */
    @Override
    @Transactional(readOnly = true)
    public List<TransferOrderDTO> getOutgoingWHTransferOrders(
            Integer warehouseId, Integer sourceWarehouseId, String status) {

        boolean hasStatus  = status != null && !status.isBlank();
        boolean hasSrc     = sourceWarehouseId != null;
        List<TransferOrder> transfers;

        if (hasStatus && hasSrc) {
            transfers = transferOrderDao.findBySourceAndDestinationWarehouseIdAndStatus(
                    sourceWarehouseId, status, warehouseId);
        } else if (hasStatus) {
            transfers = transferOrderDao.findByDestinationWarehouseIdAndStatus(warehouseId, status);
        } else if (hasSrc) {
            transfers = transferOrderDao.findBySourceAndDestinationWarehouseId(sourceWarehouseId, warehouseId);
        } else {
            transfers = transferOrderDao.findByDestinationWarehouseId(warehouseId);
        }
        fetchDetails(transfers);
        return transfers.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    /** Incoming view: source-warehouse manager sees requests they need to fulfill */
    @Override
    @Transactional(readOnly = true)
    public List<TransferOrderDTO> getIncomingWHTransferOrders(
            Integer destWarehouseId, Integer warehouseId, String status) {

        boolean hasStatus  = status != null && !status.isBlank();
        boolean hasDest    = destWarehouseId != null;
        List<TransferOrder> transfers;

        if (hasStatus && hasDest) {
            transfers = transferOrderDao.findBySourceAndDestinationWarehouseIdAndStatus(
                    warehouseId, status, destWarehouseId);
        } else if (hasStatus) {
            transfers = transferOrderDao.findBySourceWarehouseIdAndStatus(warehouseId, status);
        } else if (hasDest) {
            transfers = transferOrderDao.findBySourceAndDestinationWarehouseId(warehouseId, destWarehouseId);
        } else {
            transfers = transferOrderDao.findBySourceWarehouseId(warehouseId);
        }
        fetchDetails(transfers);
        return transfers.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TransferOrderDTO getTOById(Integer id) {
        TransferOrder tr = transferOrderDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TransferOrder not found: " + id));
        tr.setDetails(toDetailDao.findByTransferOrderId(id));
        return mapToDTO(tr);
    }

    // ==================== Save / Submit / Delete ====================

    @Override
    public void saveDraftTO(TransferOrderDTO trDTO, User user) {
        TransferOrder tr = buildEntity(trDTO, user, "Draft");
        transferOrderDao.save(tr);
        saveDetails(tr, trDTO.getDetails());
    }

    @Override
    public TransferOrderDTO submitTO(TransferOrderDTO trDTO, User user) {
        // 1. Validate user warehouse
        if (!user.getWarehouse().getWarehouseId().equals(trDTO.getDestinationWarehouseId())) {
            throw new IllegalArgumentException(
                    "You can only submit transfer requests if you're from Dest Warehouse.");
        }
        // 2. Validate details (product, UoM, qty)
        validateDetails(trDTO.getDetails());
        // 3. Check stock availability in source warehouse
        checkStockAvailability(trDTO.getSourceWarehouseId(), trDTO.getDetails());
        // 4. Build TransferOrder entity
        TransferOrder tr = buildEntity(trDTO, user, "Pending");
        // 5. Save header
        transferOrderDao.save(tr);
        // 6. Save details
        saveDetails(tr, trDTO.getDetails());
        // 7. Fetch saved details for DTO
        tr.setDetails(toDetailDao.findByTransferOrderId(tr.getToId()));
        // 8. Map to DTO and return
        return mapToDTO(tr);
    }

    @Override
    public void deleteTO(Integer id, User user) {
        TransferOrder tr = transferOrderDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TransferOrder not found: " + id));
        if (!"Draft".equals(tr.getStatus())) {
            throw new IllegalArgumentException("Only Draft Transfers can be deleted.");
        }
        if (!user.getWarehouse().getWarehouseId()
                .equals(tr.getDestinationWarehouse().getWarehouseId())) {
            throw new IllegalArgumentException(
                    "You can only delete transfer requests if you're from Dest Warehouse.");
        }
        // Details are deleted by CASCADE DELETE in SQL; just delete the header.
        transferOrderDao.deleteById(id);
    }

    // ==================== Approve ====================

    @Override
    @Transactional
    public void approveTO(Integer id, User user) {
        TransferOrder tr = transferOrderDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TransferOrder not found: " + id));

        if (!"Pending".equals(tr.getStatus())) {
            throw new IllegalArgumentException("Only Pending transfers can be approved.");
        }
        if (!user.getWarehouse().getWarehouseId()
                .equals(tr.getSourceWarehouse().getWarehouseId())) {
            throw new IllegalArgumentException("Only source warehouse can approve this transfer.");
        }

        // Manual fetch details – no Lazy Loading
        tr.setDetails(toDetailDao.findByTransferOrderId(id));

        Warehouse sourceWarehouse = tr.getSourceWarehouse();

        // FIFO stock reservation across batches + collect GIN allocations
        List<GINAllocation> ginAllocations = new ArrayList<>();

        for (TransferOrderDetail detail : tr.getDetails()) {
            Product product = detail.getProduct();
            BigDecimal cf = getConversionFactor(product, detail.getUom());
            int baseQtyNeeded = BigDecimal.valueOf(detail.getRequestedQty())
                    .multiply(cf).intValue();
            int remaining = baseQtyNeeded;

            List<StockBatch> batches = stockBatchDao
                    .findByWarehouseIdAndProductIdOrderByArrivalDateTimeAsc(
                            sourceWarehouse.getWarehouseId(), product.getProductId());

            for (StockBatch batch : batches) {
                if (remaining <= 0) break;
                int freeQty = batch.getQtyAvailable() - batch.getQtyReserved();
                if (freeQty <= 0) continue;

                int reserveQty = Math.min(freeQty, remaining);
                batch.setQtyReserved(batch.getQtyReserved() + reserveQty);
                stockBatchDao.save(batch);

                stockMovementDao.save(StockMovement.builder()
                        .warehouse(sourceWarehouse).product(product).bin(batch.getBin())
                        .batchNumber(batch.getBatchNumber())
                        .movementType("Reserve").stockType("Reserved")
                        .quantity(reserveQty).uom(product.getBaseUoM())
                        .balanceAfter(batch.getQtyReserved()).build());

                ginAllocations.add(new GINAllocation(detail, product, batch, reserveQty, detail.getUom()));
                remaining -= reserveQty;
            }

            if (remaining > 0) {
                throw new IllegalArgumentException(
                        "Not enough stock for product " + product.getProductName()
                                + ". Missing " + remaining + " " + product.getBaseUoM());
            }
        }

        transferOrderDao.updateStatus(id, "Approved");

        // Auto-create Draft GIN for the source warehouse
        GoodsIssueNote gin = new GoodsIssueNote();
        gin.setGinNumber(generateGINNumber());
        gin.setTransferOrder(tr);
        gin.setWarehouse(sourceWarehouse);
        gin.setGiStatus("Draft");

        ginDao.save(gin);   // save header first → get GINID

        for (GINAllocation alloc : ginAllocations) {
            GoodsIssueDetail ginDetail = new GoodsIssueDetail();
            ginDetail.setGoodsIssueNote(gin);
            ginDetail.setTransferOrderDetail(alloc.toDetail());
            ginDetail.setProduct(alloc.product());
            ginDetail.setIssuedQty(0);
            ginDetail.setPlannedQty(alloc.reservedQty());
            ginDetail.setUom(alloc.uom());
            ginDetail.setBatchNumber(alloc.batch().getBatchNumber());
            ginDetail.setBin(alloc.batch().getBin());
            ginDetailDao.save(ginDetail);
        }
    }

    // ==================== Reject ====================

    @Override
    public void rejectTO(Integer id, User user, String reason) {
        TransferOrder tr = transferOrderDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TransferOrder not found: " + id));
        if (!"Pending".equals(tr.getStatus())) {
            throw new IllegalArgumentException("Only Transfers with 'Pending' status can be rejected.");
        }
        if (!user.getWarehouse().getWarehouseId()
                .equals(tr.getSourceWarehouse().getWarehouseId())) {
            throw new IllegalArgumentException("Only source warehouse can reject this transfer.");
        }
        transferOrderDao.updateStatusAndRejectReason(id, "Rejected", reason);
    }

    // ==================== Helpers ====================

    /** Build a TransferOrder entity for INSERT or UPDATE from the DTO */
    private TransferOrder buildEntity(TransferOrderDTO dto, User currentUser, String status) {
        TransferOrder tr;

        if (dto.getToId() != null) {
            // Edit existing Draft/Rejected TR
            tr = transferOrderDao.findById(dto.getToId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "TransferOrder not found: " + dto.getToId()));
            if (!"Draft".equals(tr.getStatus()) && !"Rejected".equals(tr.getStatus())) {
                throw new IllegalArgumentException("Only Draft or Rejected Transfers can be edited.");
            }
            // Old details will be replaced by saveDetails(); SQL CASCADE handles orphans.
            tr.setStatus(status);
        } else {
            tr = new TransferOrder();
            tr.setToNumber(generateToNumber());
            Warehouse dest = new Warehouse();
            dest.setWarehouseId(currentUser.getWarehouse().getWarehouseId());
            tr.setDestinationWarehouse(dest);
            tr.setCreatedBy(currentUser);
            tr.setStatus(status);
        }

        if (dto.getSourceWarehouseId() != null) {
            Warehouse src = warehouseDao.findById(dto.getSourceWarehouseId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Source Warehouse not found: " + dto.getSourceWarehouseId()));
            tr.setSourceWarehouse(src);
        }

        return tr;
    }

    /** Insert or update each detail line from the DTO */
    private void saveDetails(TransferOrder tr, List<TransferOrderDetailDTO> detailDTOs) {
        if (detailDTOs == null) return;
        for (TransferOrderDetailDTO d : detailDTOs) {
            if (d.getProductId() == null) continue;
            Product product = productDao.findById(d.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Product not found: " + d.getProductId()));

            TransferOrderDetail detail = new TransferOrderDetail();
            detail.setTransferOrder(tr);
            detail.setProduct(product);
            detail.setUom(d.getUom());
            detail.setRequestedQty(d.getRequestedQty());
            detail.setIssuedQty(0);
            detail.setReceivedQty(0);
            toDetailDao.save(detail);
        }
    }

    /** Fetch child details for a list of TOs (avoids N+1 in list view) */
    private void fetchDetails(List<TransferOrder> orders) {
        for (TransferOrder to : orders) {
            to.setDetails(toDetailDao.findByTransferOrderId(to.getToId()));
        }
    }

    private void validateDetails(List<TransferOrderDetailDTO> details) {
        if (details == null || details.stream().noneMatch(d -> d.getProductId() != null)) {
            throw new IllegalArgumentException("At least one product line is required to submit.");
        }
        for (int i = 0; i < details.size(); i++) {
            TransferOrderDetailDTO d = details.get(i);
            if (d.getProductId() == null) continue;
            if (d.getUom() == null || d.getUom().isBlank()) {
                throw new IllegalArgumentException("UoM is required on line " + (i + 1) + ".");
            }
            if (d.getRequestedQty() == null || d.getRequestedQty() <= 0) {
                throw new IllegalArgumentException(
                        "Quantity must be greater than 0 on line " + (i + 1) + ".");
            }
        }
    }

    private BigDecimal getConversionFactor(Product product, String uom) {
        if (uom.equals(product.getBaseUoM())) return BigDecimal.ONE;
        return uomConversionDao.findByProductId(product.getProductId()).stream()
                .filter(c -> c.getFromUoM().equals(uom))
                .findFirst()
                .map(c -> BigDecimal.valueOf(c.getConversionFactor()))
                .orElse(BigDecimal.ONE);
    }

    private String generateToNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "TRO-" + dateStr + "-";
        String max = transferOrderDao.findMaxToNumber(prefix);
        int next = (max != null) ? Integer.parseInt(max.substring(prefix.length())) + 1 : 1;
        return prefix + String.format("%03d", next);
    }

    private String generateGINNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "GIN-" + dateStr + "-";
        String max = ginDao.findMaxGinNumber(prefix);
        int next = (max != null) ? Integer.parseInt(max.substring(prefix.length())) + 1 : 1;
        return prefix + String.format("%03d", next);
    }

    // ==================== Mapping ====================

    private TransferOrderDTO mapToDTO(TransferOrder to) {
        TransferOrderDTO dto = TransferOrderDTO.builder()
                .toId(to.getToId())
                .toNumber(to.getToNumber())
                .sourceWarehouseId(to.getSourceWarehouse().getWarehouseId())
                .sourceWarehouseName(to.getSourceWarehouse().getWarehouseName())
                .destinationWarehouseId(to.getDestinationWarehouse().getWarehouseId())
                .destinationWarehouseName(to.getDestinationWarehouse().getWarehouseName())
                .status(to.getStatus())
                .rejectReason(to.getRejectReason())
                .build();

        if (to.getDetails() != null) {
            dto.setDetails(to.getDetails().stream()
                    .map(this::mapDetailToDTO).collect(Collectors.toList()));
        }
        return dto;
    }

    private TransferOrderDetailDTO mapDetailToDTO(TransferOrderDetail detail) {
        String displayName = detail.getProduct() != null
                ? detail.getProduct().getSku() + " - " + detail.getProduct().getProductName() : "";
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

    // ── Private record for GIN batch allocation tracking ─────────────────────
    private record GINAllocation(TransferOrderDetail toDetail, Product product,
                                 StockBatch batch, int reservedQty, String uom) {}
    private void checkStockAvailability(Integer warehouseId, List<TransferOrderDetailDTO> details) {
        for (int i = 0; i < details.size(); i++) {
            TransferOrderDetailDTO d = details.get(i);
            if (d.getProductId() == null) continue;

            Product product = productDao.findById(d.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + d.getProductId()));

            BigDecimal cf = getConversionFactor(product, d.getUom());
            int neededQty = BigDecimal.valueOf(d.getRequestedQty()).multiply(cf).intValue();
            int availableQty = 0;

            // Lấy các batch trong kho theo dạng đọc bình thường không có FOR UPDATE (Tránh bottleneck chết chùm)
            List<StockBatch> batches = stockBatchDao
                    .findByWarehouseIdAndProductId(warehouseId, product.getProductId());

            for (StockBatch batch : batches) {
                int freeQty = batch.getQtyAvailable() - batch.getQtyReserved();
                if (freeQty <= 0) continue;
                availableQty += freeQty;
            }

            if (availableQty < neededQty) {
                throw new IllegalArgumentException(
                        "Not enough stock for product " + product.getProductName()
                                + ". Requested: " + neededQty + ", available: " + availableQty
                );
            }
        }
    }

}