package com.minhhai.wms.repository;

import com.minhhai.wms.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarehouseRepository extends JpaRepository<Warehouse, Integer> {

    List<Warehouse> findAllByOrderByWarehouseCodeAsc();

    boolean existsByWarehouseCode(String warehouseCode);

    boolean existsByWarehouseCodeAndWarehouseIdNot(String warehouseCode, Integer warehouseId);
}
