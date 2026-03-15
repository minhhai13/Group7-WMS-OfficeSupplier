package com.minhhai.wms.service;

import com.minhhai.wms.dto.SaleOrderDTO;
import com.minhhai.wms.dto.StockCheckResult;
import com.minhhai.wms.entity.User;

import java.util.List;
import java.util.Map;

public interface SalesOrderService {

    List<SaleOrderDTO> getSOsByWarehouse(Integer warehouseId, String status, Integer customerId);

    SaleOrderDTO getSOById(Integer soId);

    SaleOrderDTO saveDraft(SaleOrderDTO dto, User currentUser);

    /**
     * Two-step submit:
     * 1. checkAndSubmit(dto, user, false) → validates + checks ATP. If shortage,
     * returns shortages without creating PR.
     * 2. checkAndSubmit(dto, user, true) → same but creates PR for missing qty.
     * In both cases, SO moves to "Pending Approval".
     */
    StockCheckResult checkAndSubmit(SaleOrderDTO dto, User currentUser, boolean createPR);

    void deleteSO(Integer soId);

    /**
     * Approve a SO:
     * - If linked PR exists → SO="Waiting for Stock", PR="Approved"
     * - If no PR → FIFO Reserve + auto-GIN + SO="Approved"
     * Also called by GRN loopback with fromLoopback=true (accepts "Waiting for
     * Stock" status).
     */
    String approveSO(Integer soId, User currentUser);

    /**
     * Reject a SO + cascade-reject linked PRs.
     */
    void rejectSO(Integer soId, String reason);
}
