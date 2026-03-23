package com.minhhai.wms.repository;

import com.minhhai.wms.entity.StockBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface StockBatchRepository extends JpaRepository<StockBatch, Integer> {

    List<StockBatch> findByBinBinId(Integer binId);

    @Query("SELECT sb FROM StockBatch sb JOIN FETCH sb.product WHERE sb.bin.binId = :binId")
    List<StockBatch> findByBinBinIdEager(@Param("binId") Integer binId);

    List<StockBatch> findByWarehouseWarehouseId(Integer warehouseId);

    List<StockBatch> findByProductProductId(Integer productId);

    List<StockBatch> findByWarehouseWarehouseIdAndProductProductId(Integer warehouseId, Integer productId);

    java.util.Optional<StockBatch> findByWarehouse_WarehouseIdAndProduct_ProductIdAndBin_BinIdAndBatchNumber(
            Integer warehouseId, Integer productId, Integer binId, String batchNumber);

    List<StockBatch> findByWarehouse_WarehouseIdAndProduct_ProductIdOrderByArrivalDateTimeAsc(
            Integer warehouseId, Integer productId);

    @Query("SELECT SUM((s.qtyAvailable + s.qtyReserved + s.qtyInTransit) * s.product.unitWeight) " +
            "FROM StockBatch s WHERE s.bin.binId = :binId")
    BigDecimal getTotalWeightByBinId(@Param("binId") Integer binId);

    @Query("SELECT SUM(s.qtyAvailable + s.qtyReserved + s.qtyInTransit) " +
            "FROM StockBatch s WHERE s.bin.warehouse.warehouseId = :warehouseId")
    Integer getTotalQtyByWarehouseId(@Param("warehouseId") Integer warehouseId);
}
