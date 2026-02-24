package com.minhhai.wms.repository;

import com.minhhai.wms.entity.StockBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockBatchRepository extends JpaRepository<StockBatch, Integer> {

    List<StockBatch> findByBinBinId(Integer binId);

    List<StockBatch> findByWarehouseWarehouseId(Integer warehouseId);

    List<StockBatch> findByProductProductId(Integer productId);

    List<StockBatch> findByWarehouseWarehouseIdAndProductProductId(Integer warehouseId, Integer productId);
}
