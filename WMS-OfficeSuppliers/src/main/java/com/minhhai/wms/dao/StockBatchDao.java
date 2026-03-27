package com.minhhai.wms.dao;

import com.minhhai.wms.dto.CurrentStockDTO;
import com.minhhai.wms.entity.StockBatch;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

public interface StockBatchDao {

    BigDecimal getTotalWeightByBinId(Integer binId);

    Integer getTotalQtyByWarehouseId(Integer warehouseId);

    Integer getTotalQtyAvailableByBinId(Integer binId);

    Optional<StockBatch> findByWarehouseIdAndProductIdAndBinIdAndBatchNumber(
            Integer warehouseId, Integer productId, Integer binId, String batchNumber);

    List<StockBatch> findByBinId(Integer binId);

    List<StockBatch> findByWarehouseIdAndProductIdOrderByArrivalDateTimeAsc(Integer warehouseId, Integer productId);

    List<StockBatch> findByWarehouseIdAndProductId(Integer warehouseId, Integer productId);

    List<StockBatch> findByWarehouseId(Integer warehouseId);

    StockBatch save(StockBatch batch);

    /**
     * Returns the current stock snapshot per product for a given warehouse.
     * @param warehouseId  warehouse to query
     * @param lowStockOnly if true, only return products where totalAvailable < minStockLevel
     */
    List<CurrentStockDTO> findCurrentStockByWarehouse(Integer warehouseId, boolean lowStockOnly);

    /** Number of products currently below their MinStockLevel in a warehouse. */
    long countLowStockProducts(Integer warehouseId);
}
