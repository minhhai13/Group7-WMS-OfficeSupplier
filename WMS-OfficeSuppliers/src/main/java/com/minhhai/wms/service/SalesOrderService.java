package com.minhhai.wms.service;

import com.minhhai.wms.dto.SaleOrderDTO;
import com.minhhai.wms.entity.User;

import java.util.List;
import java.util.Map;

public interface SalesOrderService {

    List<SaleOrderDTO> getSOsByWarehouse(Integer warehouseId, String status, Integer customerId);

    SaleOrderDTO getSOById(Integer soId);

    SaleOrderDTO saveDraft(SaleOrderDTO dto, User currentUser);

    SaleOrderDTO submitForApproval(SaleOrderDTO dto, User currentUser);

    void deleteSO(Integer soId);

    /**
     * Approve a SO: status → Approved, reserve stock via FIFO, auto-generate GIN (Draft).
     * @return the generated GIN number for flash message
     */
    String approveSO(Integer soId, User currentUser);

    /**
     * Reject a SO: status → Rejected, save rejection reason.
     */
    void rejectSO(Integer soId, String reason);

    /**
     * Returns available UoMs for a product: BaseUoM + all FromUoMs from conversions.
     */
    List<Map<String, String>> getAvailableUoMs(Integer productId);
}
