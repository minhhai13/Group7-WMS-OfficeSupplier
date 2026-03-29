package com.minhhai.wms.dao;

import com.minhhai.wms.entity.Bin;

import java.util.List;
import java.util.Optional;

public interface BinDao {

    List<Bin> findByWarehouseId(Integer warehouseId);

    List<Bin> findByWarehouseIdAndIsActive(Integer warehouseId, Boolean isActive);

    List<Bin> findByWarehouseIdAndIsActiveForUpdate(Integer warehouseId, Boolean isActive);

    Optional<Bin> findById(Integer id);

    List<Bin> searchInWarehouse(Integer warehouseId, String keyword);

    boolean existsByWarehouseIdAndBinLocation(Integer warehouseId, String binLocation);

    boolean existsByWarehouseIdAndBinLocationAndBinIdNot(Integer warehouseId, String binLocation, Integer binId);

    Bin save(Bin bin);
}
