package com.minhhai.wms.repository;

import com.minhhai.wms.entity.Bin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BinRepository extends JpaRepository<Bin, Integer> {

    List<Bin> findByWarehouseWarehouseId(Integer warehouseId);

    List<Bin> findByWarehouseWarehouseIdAndIsActive(Integer warehouseId, Boolean isActive);

    @Query("SELECT b FROM Bin b WHERE b.warehouse.warehouseId = :warehouseId " +
            "AND LOWER(b.binLocation) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Bin> searchInWarehouse(@Param("warehouseId") Integer warehouseId, @Param("keyword") String keyword);

    @Query("SELECT b FROM Bin b WHERE b.warehouse.warehouseId = :warehouseId " +
            "AND LOWER(b.binLocation) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Bin> searchInWarehousePageable(@Param("warehouseId") Integer warehouseId,
                                        @Param("keyword") String keyword,
                                        Pageable pageable);

    Page<Bin> findByWarehouseWarehouseId(Integer warehouseId, Pageable pageable);

    boolean existsByWarehouseWarehouseIdAndBinLocation(Integer warehouseId, String binLocation);

    boolean existsByWarehouseWarehouseIdAndBinLocationAndBinIdNot(Integer warehouseId, String binLocation, Integer binId);
}
