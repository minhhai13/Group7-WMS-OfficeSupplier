package com.minhhai.wms.service;

import com.minhhai.wms.dto.WarehouseDTO;
import com.minhhai.wms.entity.Warehouse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface WarehouseService {

    List<Warehouse> findAll();

    List<Warehouse> findAllActive();

    Optional<Warehouse> findById(Integer id);

    List<Warehouse> search(String keyword);

    Warehouse save(WarehouseDTO warehouseDTO);

    Warehouse save(Warehouse warehouse);

    void toggleActive(Integer warehouseId);

    List<Warehouse> findAllActiveExcluding(Integer warehouseId);

    Page<Warehouse> findPaginated(String keyword, int page, int size);

}
