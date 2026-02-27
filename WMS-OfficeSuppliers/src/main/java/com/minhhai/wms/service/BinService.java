package com.minhhai.wms.service;

import com.minhhai.wms.dto.BinDTO;
import com.minhhai.wms.entity.Bin;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BinService {

    List<Bin> findByWarehouseId(Integer warehouseId);

    Optional<Bin> findById(Integer id);

    List<Bin> search(Integer warehouseId, String keyword);

    Bin save(BinDTO binDTO);

    Bin save(Bin bin);

    void toggleActive(Integer binId);

    /**
     * Computes current weight in a bin: SUM(stockBatch.qtyAvailable * product.unitWeight)
     */
    BigDecimal getCurrentWeight(Integer binId);

    /**
     * Returns bin.maxWeight - getCurrentWeight
     */
    BigDecimal getAvailableCapacity(Integer binId);
}
