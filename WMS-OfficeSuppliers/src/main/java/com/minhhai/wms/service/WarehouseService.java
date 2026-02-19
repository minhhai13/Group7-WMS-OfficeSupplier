package com.minhhai.wms.service;

import com.minhhai.wms.dto.WarehouseDTO;
import com.minhhai.wms.entity.Warehouse;

import java.util.List;
import java.util.Optional;

public interface WarehouseService {

    List<Warehouse> findAll();

    List<Warehouse> findAllActive();

    Optional<Warehouse> findById(Integer id);

    Warehouse save(WarehouseDTO warehouseDTO);

    Warehouse save(Warehouse warehouse);

    void toggleActive(Integer warehouseId);

    boolean existsByWarehouseCode(String code);

    boolean existsByWarehouseCodeExcluding(String code, Integer warehouseId);
}
