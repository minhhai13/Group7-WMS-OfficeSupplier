package com.minhhai.wms.dao.impl;

import com.minhhai.wms.dao.StockMovementDao;
import com.minhhai.wms.entity.Bin;
import com.minhhai.wms.entity.Product;
import com.minhhai.wms.entity.StockMovement;
import com.minhhai.wms.entity.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class StockMovementDaoImpl implements StockMovementDao {

    private final JdbcTemplate jdbcTemplate;

    public StockMovementDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ── RowMapper ─────────────────────────────────────────────────────────────

    private static final RowMapper<StockMovement> MOVEMENT_MAPPER = (rs, rowNum) -> {
        StockMovement m = new StockMovement();
        m.setMovementId(rs.getInt("MovementID"));
        m.setMovementType(rs.getString("MovementType"));
        m.setStockType(rs.getString("StockType"));
        m.setBatchNumber(rs.getString("BatchNumber"));
        m.setQuantity(rs.getInt("Quantity"));
        m.setUom(rs.getString("UoM"));
        m.setBalanceAfter(rs.getInt("BalanceAfter"));

        Timestamp ts = rs.getTimestamp("MovementDate");
        if (ts != null) m.setMovementDate(ts.toLocalDateTime());

        Warehouse wh = new Warehouse();
        wh.setWarehouseId(rs.getInt("WarehouseID"));
        wh.setWarehouseName(rs.getString("WarehouseName"));
        m.setWarehouse(wh);

        Product p = new Product();
        p.setProductId(rs.getInt("ProductID"));
        p.setProductName(rs.getString("ProductName"));
        p.setSku(rs.getString("SKU"));
        m.setProduct(p);

        Integer binId = rs.getObject("BinID", Integer.class);
        if (binId != null) {
            Bin bin = new Bin();
            bin.setBinId(binId);
            bin.setBinLocation(rs.getString("BinLocation"));
            m.setBin(bin);
        }
        return m;
    };

    private static final String MOVEMENT_BASE_SELECT = """
            SELECT m.MovementID, m.WarehouseID, m.ProductID, m.BinID, m.BatchNumber,
                   m.MovementType, m.StockType, m.MovementDate, m.Quantity, m.UoM, m.BalanceAfter,
                   w.WarehouseName,
                   p.ProductName, p.SKU,
                   b.BinLocation
            FROM StockMovements m
            JOIN Warehouses w ON m.WarehouseID = w.WarehouseID
            JOIN Products   p ON m.ProductID   = p.ProductID
            LEFT JOIN Bins  b ON m.BinID       = b.BinID
            """;

    // ── Save ─────────────────────────────────────────────────────────────────

    @Override
    public StockMovement save(StockMovement movement) {
        if (movement.getMovementId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO StockMovements (WarehouseID, ProductID, BinID, BatchNumber, MovementType, StockType, Quantity, UoM, BalanceAfter) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setInt(1, movement.getWarehouse().getWarehouseId());
                ps.setInt(2, movement.getProduct().getProductId());
                ps.setInt(3, movement.getBin().getBinId());
                ps.setString(4, movement.getBatchNumber());
                ps.setString(5, movement.getMovementType());
                ps.setString(6, movement.getStockType());
                ps.setInt(7, movement.getQuantity());
                ps.setString(8, movement.getUom());
                ps.setInt(9, movement.getBalanceAfter());
                return ps;
            }, keyHolder);
            if (keyHolder.getKey() != null) {
                movement.setMovementId(keyHolder.getKey().intValue());
            }
        }
        return movement;
    }

    // ── Inbound History (paginated) ───────────────────────────────────────────

    @Override
    public Page<StockMovement> findInboundHistory(
            LocalDateTime startDate, LocalDateTime endDate,
            Integer warehouseId, Integer productId,
            Pageable pageable) {

        return queryPaged(
                "m.MovementType IN ('Receipt', 'Transfer-In') AND m.StockType = 'Physical'",
                startDate, endDate, warehouseId, productId, pageable);
    }

    // ── Outbound History (paginated) ──────────────────────────────────────────

    @Override
    public Page<StockMovement> findOutboundHistory(
            LocalDateTime startDate, LocalDateTime endDate,
            Integer warehouseId, Integer productId,
            Pageable pageable) {

        return queryPaged(
                "m.MovementType IN ('Issue', 'Transfer-Out') AND m.StockType = 'Physical'",
                startDate, endDate, warehouseId, productId, pageable);
    }

    // ── Opening Stock (aggregate, before date) ────────────────────────────────

    @Override
    public List<Object[]> findOpeningStock(LocalDateTime beforeDate, Integer warehouseId) {
        StringBuilder sql = new StringBuilder("""
                SELECT m.ProductID, p.SKU, p.ProductName,
                       m.WarehouseID, w.WarehouseName, m.UoM,
                       SUM(CASE WHEN m.MovementType IN ('Receipt','Transfer-In') THEN m.Quantity
                                ELSE -m.Quantity END) AS OpeningQty
                FROM StockMovements m
                JOIN Products   p ON m.ProductID   = p.ProductID
                JOIN Warehouses w ON m.WarehouseID = w.WarehouseID
                WHERE m.StockType = 'Physical'
                  AND m.MovementDate < ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(Timestamp.valueOf(beforeDate));

        if (warehouseId != null) {
            sql.append(" AND m.WarehouseID = ?");
            params.add(warehouseId);
        }
        sql.append("""
                GROUP BY m.ProductID, p.SKU, p.ProductName,
                         m.WarehouseID, w.WarehouseName, m.UoM
                """);

        return jdbcTemplate.query(sql.toString(), (rs, rn) -> mapOpeningRow(rs), params.toArray());
    }

    // ── Period Summary (aggregate, in date range) ─────────────────────────────

    @Override
    public List<Object[]> findPeriodSummary(
            LocalDateTime startDate, LocalDateTime endDate, Integer warehouseId) {

        StringBuilder sql = new StringBuilder("""
                SELECT m.ProductID, p.SKU, p.ProductName,
                       m.WarehouseID, w.WarehouseName, m.UoM,
                       SUM(CASE WHEN m.MovementType IN ('Receipt','Transfer-In') THEN m.Quantity ELSE 0 END) AS InboundQty,
                       SUM(CASE WHEN m.MovementType IN ('Issue','Transfer-Out')  THEN m.Quantity ELSE 0 END) AS OutboundQty
                FROM StockMovements m
                JOIN Products   p ON m.ProductID   = p.ProductID
                JOIN Warehouses w ON m.WarehouseID = w.WarehouseID
                WHERE m.StockType = 'Physical'
                  AND m.MovementDate >= ?
                  AND m.MovementDate <= ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(Timestamp.valueOf(startDate));
        params.add(Timestamp.valueOf(endDate));

        if (warehouseId != null) {
            sql.append(" AND m.WarehouseID = ?");
            params.add(warehouseId);
        }
        sql.append("""
                GROUP BY m.ProductID, p.SKU, p.ProductName,
                         m.WarehouseID, w.WarehouseName, m.UoM
                """);

        return jdbcTemplate.query(sql.toString(), (rs, rn) -> mapPeriodRow(rs), params.toArray());
    }

    // ── Internal helper: paginated query ──────────────────────────────────────

    private Page<StockMovement> queryPaged(
            String typeFilter,
            LocalDateTime startDate, LocalDateTime endDate,
            Integer warehouseId, Integer productId,
            Pageable pageable) {

        // Build dynamic WHERE conditions
        StringBuilder where = new StringBuilder(" WHERE ").append(typeFilter);
        List<Object> params = new ArrayList<>();

        if (startDate != null) { where.append(" AND m.MovementDate >= ?"); params.add(Timestamp.valueOf(startDate)); }
        if (endDate   != null) { where.append(" AND m.MovementDate <= ?"); params.add(Timestamp.valueOf(endDate)); }
        if (warehouseId != null) { where.append(" AND m.WarehouseID = ?"); params.add(warehouseId); }
        if (productId   != null) { where.append(" AND m.ProductID   = ?"); params.add(productId); }

        // COUNT for total
        String countSql = "SELECT COUNT(1) FROM StockMovements m" + where;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        if (total == null) total = 0L;

        // Data query with ORDER BY + OFFSET/FETCH (SQL Server pagination)
        String dataSql = MOVEMENT_BASE_SELECT + where
                + " ORDER BY m.MovementDate DESC"
                + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add((long) pageable.getOffset());
        dataParams.add(pageable.getPageSize());

        List<StockMovement> content = jdbcTemplate.query(dataSql, MOVEMENT_MAPPER, dataParams.toArray());
        return new PageImpl<>(content, pageable, total);
    }

    // ── Row extractor helpers for aggregate queries ───────────────────────────

    private Object[] mapOpeningRow(ResultSet rs) throws SQLException {
        return new Object[]{
                rs.getInt("ProductID"),
                rs.getString("SKU"),
                rs.getString("ProductName"),
                rs.getInt("WarehouseID"),
                rs.getString("WarehouseName"),
                rs.getString("UoM"),
                rs.getInt("OpeningQty")
        };
    }

    private Object[] mapPeriodRow(ResultSet rs) throws SQLException {
        return new Object[]{
                rs.getInt("ProductID"),
                rs.getString("SKU"),
                rs.getString("ProductName"),
                rs.getInt("WarehouseID"),
                rs.getString("WarehouseName"),
                rs.getString("UoM"),
                rs.getInt("InboundQty"),
                rs.getInt("OutboundQty")
        };
    }
}
