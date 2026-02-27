package com.minhhai.wms.repository;

import com.minhhai.wms.entity.StockBatch;
import com.minhhai.wms.dto.PhysicalInventoryDTO; // Nhớ import DTO

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.List;

@Repository
public interface StockBatchRepository extends JpaRepository<StockBatch, Integer> {

    List<StockBatch> findByBinBinId(Integer binId);
    List<StockBatch> findByWarehouseWarehouseId(Integer warehouseId);
    List<StockBatch> findByProductProductId(Integer productId);
    List<StockBatch> findByWarehouseWarehouseIdAndProductProductId(Integer warehouseId, Integer productId);
    java.util.Optional<StockBatch> findByWarehouse_WarehouseIdAndProduct_ProductIdAndBin_BinIdAndBatchNumber(
            Integer warehouseId, Integer productId, Integer binId, String batchNumber);
    List<StockBatch> findByWarehouse_WarehouseIdAndProduct_ProductIdOrderByArrivalDateTimeAsc(
            Integer warehouseId, Integer productId);

    @Query("SELECT new com.minhhai.wms.dto.PhysicalInventoryDTO(" +
            "p.sku, p.productName, b.binLocation, sb.batchNumber, sb.qtyAvailable, sb.uom) " +
            "FROM StockBatch sb JOIN sb.product p JOIN sb.bin b " +
            "WHERE sb.warehouse.warehouseId = :warehouseId AND sb.qtyAvailable > 0 " +
            "ORDER BY p.sku, b.binLocation")
    Page<PhysicalInventoryDTO> getPhysicalInventoryByWarehouse(@Param("warehouseId") Integer warehouseId, Pageable pageable);
}