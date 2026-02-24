package com.minhhai.wms.repository;

import com.minhhai.wms.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Integer> {
    List<PurchaseOrder> findByWarehouse_WarehouseIdOrderByPoIdDesc(Integer warehouseId);
    List<PurchaseOrder> findByWarehouse_WarehouseIdAndPoStatusOrderByPoIdDesc(Integer warehouseId, String poStatus);
    boolean existsByPoNumber(String poNumber);
}
