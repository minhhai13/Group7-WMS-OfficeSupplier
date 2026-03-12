package com.minhhai.wms.service;

import com.minhhai.wms.dto.PurchaseOrderDTO;
import com.minhhai.wms.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface PurchaseOrderService {

    Page<PurchaseOrderDTO> getPOsByWarehouse(Integer warehouseId, String status, Integer supplierId, Pageable pageable);

    PurchaseOrderDTO getPOById(Integer poId);

    PurchaseOrderDTO saveDraft(PurchaseOrderDTO dto, User currentUser);

    PurchaseOrderDTO submitForApproval(PurchaseOrderDTO dto, User currentUser);

    void deletePO(Integer poId);

    /**
     * Approve a PO: status → Approved, auto-generate GRN (Draft) with BatchNumber and Bin.
     * NO stock updates (StockBatch/StockMovement) — that happens in Phase 3 (Storekeeper Post).
     * @return the generated GRN number for flash message
     */
    String approvePO(Integer poId, User currentUser);

    /**
     * Reject a PO: status → Rejected, save rejection reason.
     */
    void rejectPO(Integer poId, String reason);

    /**
     * Returns available UoMs for a product: BaseUoM + all FromUoMs from conversions.
     */
}
