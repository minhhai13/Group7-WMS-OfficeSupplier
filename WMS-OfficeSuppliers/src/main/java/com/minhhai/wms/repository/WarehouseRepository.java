package com.minhhai.wms.repository;

import com.minhhai.wms.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Integer> {

    Optional<Warehouse> findByWarehouseCode(String warehouseCode);

    boolean existsByWarehouseCode(String warehouseCode);

    boolean existsByWarehouseCodeAndWarehouseIdNot(String warehouseCode, Integer warehouseId);

    List<Warehouse> findByIsActive(Boolean isActive);
}
