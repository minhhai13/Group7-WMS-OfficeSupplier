package com.minhhai.wms.repository;

import com.minhhai.wms.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Integer> {

    List<StockMovement> findByWarehouse_WarehouseId(Integer warehouseId);

    List<StockMovement> findByProduct_ProductId(Integer productId);

    // THÊM HÀM NÀY CHO UC30 (Thẻ kho)
    @Query("SELECT sm FROM StockMovement sm " +
            "WHERE sm.warehouse.warehouseId = :warehouseId AND sm.product.productId = :productId " +
            "ORDER BY sm.movementDate ASC")
    List<StockMovement> getMovementsByProductAndWarehouse(@Param("productId") Integer productId, @Param("warehouseId") Integer warehouseId);
}