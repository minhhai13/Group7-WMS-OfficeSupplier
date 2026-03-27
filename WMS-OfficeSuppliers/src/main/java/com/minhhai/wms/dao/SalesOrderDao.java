package com.minhhai.wms.dao;

import com.minhhai.wms.entity.SalesOrder;

import java.util.List;
import java.util.Optional;

public interface SalesOrderDao {

    List<SalesOrder> findByWarehouseId(Integer warehouseId);

    List<SalesOrder> findByWarehouseIdAndStatus(Integer warehouseId, String status);

    List<SalesOrder> findByWarehouseIdAndCustomerId(Integer warehouseId, Integer customerId);

    List<SalesOrder> findByWarehouseIdAndStatusAndCustomerId(Integer warehouseId, String status, Integer customerId);

    Optional<SalesOrder> findById(Integer soId);

    SalesOrder save(SalesOrder salesOrder);

    void deleteById(Integer soId);

    String findMaxSoNumber(String prefix);
}
