package com.minhhai.wms.repository;

import com.minhhai.wms.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Integer> {

    List<PurchaseOrder> findByWarehouse_WarehouseId(Integer warehouseId);

    List<PurchaseOrder> findByWarehouse_WarehouseIdAndPoStatus(Integer warehouseId, String poStatus);

    List<PurchaseOrder> findByWarehouse_WarehouseIdAndSupplier_PartnerId(Integer warehouseId, Integer supplierId);

    List<PurchaseOrder> findByWarehouse_WarehouseIdAndPoStatusAndSupplier_PartnerId(
            Integer warehouseId, String poStatus, Integer supplierId);

    @Query("SELECT MAX(po.poNumber) FROM PurchaseOrder po WHERE po.poNumber LIKE :prefix%")
    String findMaxPoNumber(@Param("prefix") String prefix);
}
