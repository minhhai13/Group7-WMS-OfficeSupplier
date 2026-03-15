package com.minhhai.wms.repository;

import com.minhhai.wms.entity.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Integer> {

    List<SalesOrder> findByWarehouse_WarehouseId(Integer warehouseId);

    List<SalesOrder> findByWarehouse_WarehouseIdAndSoStatus(Integer warehouseId, String soStatus);

    List<SalesOrder> findByWarehouse_WarehouseIdAndCustomer_PartnerId(Integer warehouseId, Integer customerId);

    List<SalesOrder> findByWarehouse_WarehouseIdAndSoStatusAndCustomer_PartnerId(
            Integer warehouseId, String soStatus, Integer customerId);

    @Query("SELECT MAX(so.soNumber) FROM SalesOrder so WHERE so.soNumber LIKE :prefix%")
    String findMaxSoNumber(@Param("prefix") String prefix);
}
