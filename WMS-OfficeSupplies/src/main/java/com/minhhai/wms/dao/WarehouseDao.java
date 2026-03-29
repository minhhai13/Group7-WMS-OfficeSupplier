package com.minhhai.wms.dao;

import com.minhhai.wms.entity.Warehouse;

import java.util.List;
import java.util.Optional;

public interface WarehouseDao {

    List<Warehouse> findAll();

    List<Warehouse> findByIsActive(Boolean isActive);

    Optional<Warehouse> findById(Integer id);

    boolean existsByWarehouseCode(String warehouseCode);

    boolean existsByWarehouseCodeAndWarehouseIdNot(String warehouseCode, Integer warehouseId);

    List<Warehouse> searchByKeyword(String keyword);

    Warehouse save(Warehouse warehouse);

    List<Warehouse> findByIsActiveTrueAndWarehouseIdNot(Integer warehouseId);
}
