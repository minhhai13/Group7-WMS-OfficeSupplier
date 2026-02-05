package com.minhhai.wms.service;

import com.minhhai.wms.entity.Warehouse;

import java.util.List;
import java.util.Optional;

public interface WarehouseService {

    List<Warehouse> findAll();

    Optional<Warehouse> findById(Integer id);

    Warehouse save(Warehouse warehouse);

    void deleteById(Integer id);

    boolean existsByWarehouseCode(String warehouseCode);

    boolean existsByWarehouseCodeExcludingId(String warehouseCode, Integer excludeWarehouseId);
}
