package com.minhhai.wms.repository;

import com.minhhai.wms.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Integer> {

    Page<PurchaseOrder> findByWarehouse_WarehouseId(Integer warehouseId, Pageable pageable);

    Page<PurchaseOrder> findByWarehouse_WarehouseIdAndPoStatus(Integer warehouseId, String poStatus, Pageable pageable);

    Page<PurchaseOrder> findByWarehouse_WarehouseIdAndSupplier_PartnerId(Integer warehouseId, Integer supplierId, Pageable pageable);

    Page<PurchaseOrder> findByWarehouse_WarehouseIdAndPoStatusAndSupplier_PartnerId(
            Integer warehouseId, String poStatus, Integer supplierId, Pageable pageable);

    @Query("SELECT MAX(po.poNumber) FROM PurchaseOrder po WHERE po.poNumber LIKE :prefix%")
    String findMaxPoNumber(@Param("prefix") String prefix);
}