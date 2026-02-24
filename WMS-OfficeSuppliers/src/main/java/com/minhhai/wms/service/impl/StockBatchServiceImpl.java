package com.minhhai.wms.service.impl;

import com.minhhai.wms.entity.StockBatch;
import com.minhhai.wms.repository.StockBatchRepository;
import com.minhhai.wms.service.StockBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockBatchServiceImpl implements StockBatchService {

    private final StockBatchRepository stockBatchRepository;

    @Override
    public List<StockBatch> findByBinId(Integer binId) {
        return stockBatchRepository.findByBinBinId(binId);
    }

    @Override
    public List<StockBatch> findByWarehouseId(Integer warehouseId) {
        return stockBatchRepository.findByWarehouseWarehouseId(warehouseId);
    }
}
