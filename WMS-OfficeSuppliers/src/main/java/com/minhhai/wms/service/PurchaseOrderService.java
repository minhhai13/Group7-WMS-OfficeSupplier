package com.minhhai.wms.service;

import com.minhhai.wms.dto.PurchaseOrderDTO;
import com.minhhai.wms.entity.PurchaseOrder;

import java.util.List;

public interface PurchaseOrderService {
    List<PurchaseOrder> getPOsByWarehouse(Integer warehouseId, String status);
    PurchaseOrder getPOById(Integer poId);
    PurchaseOrder savePO(PurchaseOrderDTO dto, Integer warehouseId, boolean isDraft);
    void submitPOForApproval(Integer poId, Integer warehouseId);
    void approvePO(Integer poId, Integer warehouseId);
    void rejectPO(Integer poId, Integer warehouseId);
    void deletePO(Integer poId, Integer warehouseId);
}
