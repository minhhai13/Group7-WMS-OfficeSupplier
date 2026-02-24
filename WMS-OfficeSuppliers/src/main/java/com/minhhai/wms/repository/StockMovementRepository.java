package com.minhhai.wms.repository;

import com.minhhai.wms.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Integer> {

    List<StockMovement> findByWarehouse_WarehouseId(Integer warehouseId);

    List<StockMovement> findByProduct_ProductId(Integer productId);
}
