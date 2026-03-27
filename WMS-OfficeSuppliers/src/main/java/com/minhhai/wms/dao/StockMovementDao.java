package com.minhhai.wms.dao;

import com.minhhai.wms.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface StockMovementDao {

    StockMovement save(StockMovement movement);

    /**
     * Paginated inbound history (Receipt, Transfer-In / Physical).
     * Optional filters: date range, warehouseId, productId.
     */
    Page<StockMovement> findInboundHistory(
            LocalDateTime startDate, LocalDateTime endDate,
            Integer warehouseId, Integer productId,
            Pageable pageable);

    /**
     * Paginated outbound history (Issue, Transfer-Out / Physical).
     * Optional filters: date range, warehouseId, productId.
     */
    Page<StockMovement> findOutboundHistory(
            LocalDateTime startDate, LocalDateTime endDate,
            Integer warehouseId, Integer productId,
            Pageable pageable);

    /**
     * Opening stock per product+warehouse: sum(Receipt/Transfer-In minus Issue/Transfer-Out) BEFORE beforeDate.
     * Returns raw rows: { productId, sku, productName, warehouseId, warehouseName, uom, openingQty }
     */
    List<Object[]> findOpeningStock(LocalDateTime beforeDate, Integer warehouseId);

    /**
     * Period summary per product+warehouse in [startDate, endDate].
     * Returns raw rows: { productId, sku, productName, warehouseId, warehouseName, uom, inboundQty, outboundQty }
     */
    List<Object[]> findPeriodSummary(
            LocalDateTime startDate, LocalDateTime endDate, Integer warehouseId);
}
