package com.minhhai.wms.repository;

import com.minhhai.wms.entity.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Integer> {

        boolean existsByWarehouseCode(String warehouseCode);

        boolean existsByWarehouseCodeAndWarehouseIdNot(String warehouseCode, Integer warehouseId);

        List<Warehouse> findByIsActive(Boolean isActive);

        @Query("SELECT w FROM Warehouse w WHERE LOWER(w.warehouseName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(w.warehouseCode) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        List<Warehouse> searchByKeyword(@Param("keyword") String keyword);

        @Query("SELECT w FROM Warehouse w WHERE LOWER(w.warehouseName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(w.warehouseCode) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        Page<Warehouse> searchByKeywordPageable(@Param("keyword") String keyword, Pageable pageable);

        List<Warehouse> findByIsActiveTrueAndWarehouseIdNot(Integer warehouseId);
}
