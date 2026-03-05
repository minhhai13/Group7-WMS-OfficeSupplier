package com.minhhai.wms.repository;

import com.minhhai.wms.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Integer> {

    List<StockMovement> findByWarehouse_WarehouseId(Integer warehouseId);

    List<StockMovement> findByProduct_ProductId(Integer productId);

    /**
     * Inbound history report:
     * - movementType = 'Receipt', stockType = 'Physical'
     * - Optional filters: date range, warehouseId, productId
     * - Ordered by movementDate DESC (newest first)
     */
    @Query("SELECT m FROM StockMovement m " +
    "LEFT JOIN FETCH m.warehouse " +
       "LEFT JOIN FETCH m.product " +
       "LEFT JOIN FETCH m.bin " +
           "WHERE m.movementType = 'Receipt' " +
           "AND m.stockType = 'Physical' " +
           "AND (:startDate IS NULL OR m.movementDate >= :startDate) " +
           "AND (:endDate IS NULL OR m.movementDate <= :endDate) " +
           "AND (:warehouseId IS NULL OR m.warehouse.warehouseId = :warehouseId) " +
           "AND (:productId IS NULL OR m.product.productId = :productId) " +
           "ORDER BY m.movementDate DESC")
    Page<StockMovement> findInboundHistory(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("warehouseId") Integer warehouseId,
            @Param("productId") Integer productId,
            Pageable pageable);

    /**
     * Outbound history report:
     * - movementType = 'Issue', stockType = 'Physical'
     * - Optional filters: date range, warehouseId, productId
     * - Ordered by movementDate DESC (newest first)
     */
    @Query("SELECT m FROM StockMovement m " +
           "LEFT JOIN FETCH m.warehouse " +
           "LEFT JOIN FETCH m.product " +
           "LEFT JOIN FETCH m.bin " +
           "WHERE m.movementType = 'Issue' " +
           "AND m.stockType = 'Physical' " +
           "AND (:startDate IS NULL OR m.movementDate >= :startDate) " +
           "AND (:endDate IS NULL OR m.movementDate <= :endDate) " +
           "AND (:warehouseId IS NULL OR m.warehouse.warehouseId = :warehouseId) " +
           "AND (:productId IS NULL OR m.product.productId = :productId) " +
           "ORDER BY m.movementDate DESC")
    Page<StockMovement> findOutboundHistory(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("warehouseId") Integer warehouseId,
            @Param("productId") Integer productId,
            Pageable pageable);

    /**
     * Opening stock per product+warehouse: Sum(Receipt - Issue) BEFORE beforeDate.
     * Returns Object[] { productId, sku, productName, warehouseId, warehouseName, uom, openingQty }
     * stockType = 'Physical' only.
     */
    @Query("SELECT m.product.productId, m.product.sku, m.product.productName, " +
           "       m.warehouse.warehouseId, m.warehouse.warehouseName, m.uom, " +
           "       SUM(CASE WHEN m.movementType IN ('Receipt', 'Transfer-In') THEN m.quantity ELSE -m.quantity END) " +
           "FROM StockMovement m " +
           "WHERE m.stockType = 'Physical' " +
           "AND m.movementDate < :beforeDate " +
           "AND (:warehouseId IS NULL OR m.warehouse.warehouseId = :warehouseId) " +
           "GROUP BY m.product.productId, m.product.sku, m.product.productName, " +
           "         m.warehouse.warehouseId, m.warehouse.warehouseName, m.uom")
    List<Object[]> findOpeningStock(
            @Param("beforeDate") LocalDateTime beforeDate,
            @Param("warehouseId") Integer warehouseId);

    /**
     * Period summary per product+warehouse: Sum of receipts and issues in [startDate, endDate].
     * Returns Object[] { productId, sku, productName, warehouseId, warehouseName, uom, inboundQty, outboundQty }
     * stockType = 'Physical' only. Includes Transfer-In/Transfer-Out.
     */
    @Query("SELECT m.product.productId, m.product.sku, m.product.productName, " +
           "       m.warehouse.warehouseId, m.warehouse.warehouseName, m.uom, " +
           "       SUM(CASE WHEN m.movementType IN ('Receipt', 'Transfer-In') THEN m.quantity ELSE 0 END), " +
           "       SUM(CASE WHEN m.movementType IN ('Issue', 'Transfer-Out')  THEN m.quantity ELSE 0 END) " +
           "FROM StockMovement m " +
           "WHERE m.stockType = 'Physical' " +
           "AND m.movementDate >= :startDate AND m.movementDate <= :endDate " +
           "AND (:warehouseId IS NULL OR m.warehouse.warehouseId = :warehouseId) " +
           "GROUP BY m.product.productId, m.product.sku, m.product.productName, " +
           "         m.warehouse.warehouseId, m.warehouse.warehouseName, m.uom")
    List<Object[]> findPeriodSummary(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("warehouseId") Integer warehouseId);
}

