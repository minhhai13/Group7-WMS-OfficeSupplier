package com.minhhai.wms.repository;

import com.minhhai.wms.entity.Bin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BinRepository extends JpaRepository<Bin, Integer> {

    List<Bin> findByWarehouseWarehouseId(Integer warehouseId);

    List<Bin> findByWarehouseWarehouseIdAndIsActiveTrue(Integer warehouseId);

    boolean existsByWarehouseWarehouseIdAndBinLocation(Integer warehouseId, String binLocation);

    boolean existsByWarehouseWarehouseIdAndBinLocationAndBinIdNot(Integer warehouseId, String binLocation, Integer binId);
}
