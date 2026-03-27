package com.minhhai.wms.dao;

import com.minhhai.wms.entity.PurchaseOrder;

import java.util.List;
import java.util.Optional;

public interface PurchaseOrderDao {

    List<PurchaseOrder> findByWarehouseId(Integer warehouseId);

    List<PurchaseOrder> findByWarehouseIdAndStatus(Integer warehouseId, String status);

    List<PurchaseOrder> findByWarehouseIdAndSupplierId(Integer warehouseId, Integer supplierId);

    List<PurchaseOrder> findByWarehouseIdAndStatusAndSupplierId(Integer warehouseId, String status, Integer supplierId);

    Optional<PurchaseOrder> findById(Integer poId);

    PurchaseOrder save(PurchaseOrder purchaseOrder);

    void deleteById(Integer poId);

    String findMaxPoNumber(String prefix);
}
