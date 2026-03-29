package com.minhhai.wms.dao;

import com.minhhai.wms.entity.PurchaseRequest;

import java.util.List;
import java.util.Optional;

public interface PurchaseRequestDao {

    List<PurchaseRequest> findByPurchaseOrderId(Integer poId);

    List<PurchaseRequest> findByWarehouseId(Integer warehouseId);

    List<PurchaseRequest> findByWarehouseIdAndStatus(Integer warehouseId, String status);

    Optional<PurchaseRequest> findById(Integer prId);

    Optional<PurchaseRequest> findByRelatedSalesOrderId(Integer soId);

    List<PurchaseRequest> findByRelatedSalesOrderIdAndStatusIn(Integer soId, List<String> statuses);

    PurchaseRequest save(PurchaseRequest purchaseRequest);

    void deleteById(Integer prId);

    String findMaxPrNumber(String prefix);

}
