package com.minhhai.wms.service;

import com.minhhai.wms.entity.StockBatch;

import java.util.List;

public interface StockBatchService {

    List<StockBatch> findByBinId(Integer binId);

    List<StockBatch> findByWarehouseId(Integer warehouseId);
}
