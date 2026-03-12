package com.minhhai.wms.repository;

import com.minhhai.wms.entity.SalesOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Integer> {

    Page<SalesOrder> findByWarehouse_WarehouseId(Integer warehouseId, Pageable pageable);

    Page<SalesOrder> findByWarehouse_WarehouseIdAndSoStatus(Integer warehouseId, String soStatus, Pageable pageable);

    Page<SalesOrder> findByWarehouse_WarehouseIdAndCustomer_PartnerId(Integer warehouseId, Integer customerId, Pageable pageable);

    Page<SalesOrder> findByWarehouse_WarehouseIdAndSoStatusAndCustomer_PartnerId(
            Integer warehouseId, String soStatus, Integer customerId, Pageable pageable);

    @Query("SELECT MAX(so.soNumber) FROM SalesOrder so WHERE so.soNumber LIKE :prefix%")
    String findMaxSoNumber(@Param("prefix") String prefix);
}