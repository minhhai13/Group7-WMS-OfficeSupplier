package com.minhhai.wms.dao.impl;

import com.minhhai.wms.dao.StockBatchDao;
import com.minhhai.wms.dao.mapper.StockBatchRowMapper;
import com.minhhai.wms.dto.CurrentStockDTO;
import com.minhhai.wms.entity.StockBatch;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public class StockBatchDaoImpl implements StockBatchDao {

    private final JdbcTemplate jdbcTemplate;
    private final StockBatchRowMapper rowMapper = new StockBatchRowMapper();

    public StockBatchDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public BigDecimal getTotalWeightByBinId(Integer binId) {
        BigDecimal total = jdbcTemplate.queryForObject(
                "SELECT SUM((sb.QtyAvailable + sb.QtyReserved + sb.QtyInTransit) * p.UnitWeight) " +
                        "FROM StockBatches sb JOIN Products p ON sb.ProductID = p.ProductID WHERE sb.BinID = ?",
                BigDecimal.class,
                binId
        );
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    public Integer getTotalQtyByWarehouseId(Integer warehouseId) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT SUM(QtyAvailable + QtyReserved + QtyInTransit) FROM StockBatches WHERE WarehouseID = ?",
                Integer.class,
                warehouseId
        );
        return total != null ? total : 0;
    }

    @Override
    public Integer getTotalQtyAvailableByBinId(Integer binId) {
        Integer total = jdbcTemplate.queryForObject(
                "SELECT SUM(QtyAvailable) FROM StockBatches WHERE BinID = ?",
                Integer.class,
                binId
        );
        return total != null ? total : 0;
    }

    @Override
    public Optional<StockBatch> findByWarehouseIdAndProductIdAndBinIdAndBatchNumber(
            Integer warehouseId, Integer productId, Integer binId, String batchNumber
    ) {
        String sql = """
                SELECT sb.StockBatchID, sb.WarehouseID, sb.ProductID, sb.BinID, sb.BatchNumber, sb.ArrivalDateTime,
                       sb.QtyAvailable, sb.QtyReserved, sb.QtyInTransit, sb.UoM,
                       p.SKU, p.ProductName, p.UnitWeight, p.BaseUoM,
                       b.BinLocation
                FROM StockBatches sb
                JOIN Products p ON sb.ProductID = p.ProductID
                JOIN Bins b ON sb.BinID = b.BinID
                WHERE sb.WarehouseID = ? AND sb.ProductID = ? AND sb.BinID = ? AND sb.BatchNumber = ?
                """;
        List<StockBatch> list = jdbcTemplate.query(sql, rowMapper, warehouseId, productId, binId, batchNumber);
        return list.stream().findFirst();
    }

    @Override
    public List<StockBatch> findByBinId(Integer binId) {
        String sql = """
                SELECT sb.StockBatchID, sb.WarehouseID, sb.ProductID, sb.BinID, sb.BatchNumber, sb.ArrivalDateTime,
                       sb.QtyAvailable, sb.QtyReserved, sb.QtyInTransit, sb.UoM,
                       p.SKU, p.ProductName, p.UnitWeight, p.BaseUoM,
                       b.BinLocation
                FROM StockBatches sb
                JOIN Products p ON sb.ProductID = p.ProductID
                JOIN Bins b ON sb.BinID = b.BinID
                WHERE sb.BinID = ?
                """;
        return jdbcTemplate.query(sql, rowMapper, binId);
    }

    @Override
    public List<StockBatch> findByWarehouseIdAndProductIdOrderByArrivalDateTimeAsc(Integer warehouseId, Integer productId) {
        String sql = """
                SELECT sb.StockBatchID, sb.WarehouseID, sb.ProductID, sb.BinID, sb.BatchNumber, sb.ArrivalDateTime,
                       sb.QtyAvailable, sb.QtyReserved, sb.QtyInTransit, sb.UoM,
                       p.SKU, p.ProductName, p.UnitWeight, p.BaseUoM,
                       b.BinLocation
                FROM StockBatches sb
                JOIN Products p ON sb.ProductID = p.ProductID
                JOIN Bins b ON sb.BinID = b.BinID
                WHERE sb.WarehouseID = ? AND sb.ProductID = ?
                ORDER BY sb.ArrivalDateTime ASC
                """;
        return jdbcTemplate.query(sql, rowMapper, warehouseId, productId);
    }

    @Override
    public List<StockBatch> findByWarehouseIdAndProductId(Integer warehouseId, Integer productId) {
        String sql = """
                SELECT sb.StockBatchID, sb.WarehouseID, sb.ProductID, sb.BinID, sb.BatchNumber, sb.ArrivalDateTime,
                       sb.QtyAvailable, sb.QtyReserved, sb.QtyInTransit, sb.UoM,
                       p.SKU, p.ProductName, p.UnitWeight, p.BaseUoM,
                       b.BinLocation
                FROM StockBatches sb
                JOIN Products p ON sb.ProductID = p.ProductID
                JOIN Bins b ON sb.BinID = b.BinID
                WHERE sb.WarehouseID = ? AND sb.ProductID = ?
                """;
        return jdbcTemplate.query(sql, rowMapper, warehouseId, productId);
    }

    @Override
    public List<StockBatch> findByWarehouseId(Integer warehouseId) {
        String sql = """
                SELECT sb.*, 
                       p.product_code, p.product_name, p.base_uom, p.min_stock_level, p.unit_weight,
                       b.bin_code, b.is_active as bin_is_active,
                       w.warehouse_id as bin_warehouse_id
                FROM StockBatches sb
                JOIN Products p ON sb.ProductID = p.ProductID
                JOIN Bins b ON sb.BinID = b.BinID
                JOIN Warehouses w ON b.WarehouseID = w.WarehouseID
                WHERE sb.WarehouseID = ?
                """;
        return jdbcTemplate.query(sql, rowMapper, warehouseId);
    }

    @Override
    public StockBatch save(StockBatch batch) {
        if (batch.getStockBatchId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO StockBatches (WarehouseID, ProductID, BinID, BatchNumber, ArrivalDateTime, QtyAvailable, QtyReserved, QtyInTransit, UoM) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setInt(1, batch.getWarehouse().getWarehouseId());
                ps.setInt(2, batch.getProduct().getProductId());
                ps.setInt(3, batch.getBin().getBinId());
                ps.setString(4, batch.getBatchNumber());
                ps.setTimestamp(5, java.sql.Timestamp.valueOf(batch.getArrivalDateTime()));
                ps.setInt(6, batch.getQtyAvailable());
                ps.setInt(7, batch.getQtyReserved());
                ps.setInt(8, batch.getQtyInTransit());
                ps.setString(9, batch.getUom());
                return ps;
            }, keyHolder);
            if (keyHolder.getKey() != null) {
                batch.setStockBatchId(keyHolder.getKey().intValue());
            }
            return batch;
        }
        jdbcTemplate.update(
                "UPDATE StockBatches SET QtyAvailable = ?, QtyReserved = ?, QtyInTransit = ?, ArrivalDateTime = ?, UoM = ? WHERE StockBatchID = ?",
                batch.getQtyAvailable(),
                batch.getQtyReserved(),
                batch.getQtyInTransit(),
                java.sql.Timestamp.valueOf(batch.getArrivalDateTime()),
                batch.getUom(),
                batch.getStockBatchId()
        );
        return batch;
    }

    // ── Current Stock Level ───────────────────────────────────────────────────

    @Override
    public List<CurrentStockDTO> findCurrentStockByWarehouse(Integer warehouseId, boolean lowStockOnly) {
        String sql = """
                SELECT p.ProductID, p.SKU, p.ProductName, p.BaseUoM, p.MinStockLevel,
                       COALESCE(SUM(sb.QtyAvailable), 0)                       AS TotalAvailable,
                       COALESCE(SUM(sb.QtyReserved), 0)                        AS TotalReserved,
                       COALESCE(SUM(sb.QtyAvailable) - SUM(sb.QtyReserved), 0) AS FreeStock,
                       COALESCE(SUM(sb.QtyInTransit), 0)                       AS TotalInTransit
                FROM Products p
                LEFT JOIN StockBatches sb ON p.ProductID = sb.ProductID AND sb.WarehouseID = ?
                WHERE p.IsActive = 1
                GROUP BY p.ProductID, p.SKU, p.ProductName, p.BaseUoM, p.MinStockLevel
                """ +
                (lowStockOnly ? " HAVING COALESCE(SUM(sb.QtyAvailable), 0) < COALESCE(p.MinStockLevel, 0)" : "") +
                " ORDER BY p.ProductName";

        return jdbcTemplate.query(sql, (rs, rn) -> {
            CurrentStockDTO dto = new CurrentStockDTO();
            dto.setProductId(rs.getInt("ProductID"));
            dto.setSku(rs.getString("SKU"));
            dto.setProductName(rs.getString("ProductName"));
            dto.setUom(rs.getString("BaseUoM"));
            dto.setMinStockLevel(rs.getObject("MinStockLevel", Integer.class));
            dto.setTotalAvailable(rs.getInt("TotalAvailable"));
            dto.setTotalReserved(rs.getInt("TotalReserved"));
            dto.setFreeStock(rs.getInt("FreeStock"));
            dto.setTotalInTransit(rs.getInt("TotalInTransit"));
            return dto;
        }, warehouseId);
    }

    @Override
    public long countLowStockProducts(Integer warehouseId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM (
                    SELECT p.ProductID
                    FROM Products p
                    LEFT JOIN StockBatches sb ON p.ProductID = sb.ProductID AND sb.WarehouseID = ?
                    WHERE p.IsActive = 1
                      AND p.MinStockLevel > 0
                    GROUP BY p.ProductID, p.MinStockLevel
                    HAVING COALESCE(SUM(sb.QtyAvailable), 0) < p.MinStockLevel
                ) AS low
                """, Long.class, warehouseId);
        return count != null ? count : 0L;
    }
}

