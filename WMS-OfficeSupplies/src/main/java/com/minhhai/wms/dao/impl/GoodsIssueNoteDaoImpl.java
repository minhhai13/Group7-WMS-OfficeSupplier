package com.minhhai.wms.dao.impl;

import com.minhhai.wms.dao.GoodsIssueNoteDao;
import com.minhhai.wms.dao.mapper.GoodsIssueNoteRowMapper;
import com.minhhai.wms.entity.GoodsIssueNote;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class GoodsIssueNoteDaoImpl implements GoodsIssueNoteDao {

    private static final String BASE_SELECT = """
            SELECT g.GINID, g.GINNumber, g.SOID, g.TransferOrderID, g.WarehouseID, g.GIStatus,
                   so.SONumber, so.CustomerID,
                   c.PartnerName AS CustomerName,
                   t.TONumber, t.DestinationWarehouseID,
                   dw.WarehouseName AS DestinationWarehouseName,
                   w.WarehouseName
            FROM GoodsIssueNotes g
            JOIN Warehouses w ON g.WarehouseID = w.WarehouseID
            LEFT JOIN SalesOrders so ON g.SOID = so.SOID
            LEFT JOIN Partners c ON so.CustomerID = c.PartnerID
            LEFT JOIN TransferOrders t ON g.TransferOrderID = t.TOID
            LEFT JOIN Warehouses dw ON t.DestinationWarehouseID = dw.WarehouseID
            """;

    private final JdbcTemplate jdbcTemplate;
    private final GoodsIssueNoteRowMapper rowMapper = new GoodsIssueNoteRowMapper();

    public GoodsIssueNoteDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<GoodsIssueNote> findByWarehouseId(Integer warehouseId) {
        return jdbcTemplate.query(BASE_SELECT + " WHERE g.WarehouseID = ?", rowMapper, warehouseId);
    }

    @Override
    public List<GoodsIssueNote> findByWarehouseIdAndStatus(Integer warehouseId, String status) {
        return jdbcTemplate.query(
                BASE_SELECT + " WHERE g.WarehouseID = ? AND g.GIStatus = ?",
                rowMapper,
                warehouseId,
                status
        );
    }

    @Override
    public Optional<GoodsIssueNote> findById(Integer ginId) {
        List<GoodsIssueNote> list = jdbcTemplate.query(BASE_SELECT + " WHERE g.GINID = ?", rowMapper, ginId);
        return list.stream().findFirst();
    }

    @Override
    public GoodsIssueNote save(GoodsIssueNote gin) {
        if (gin.getGinId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO GoodsIssueNotes (GINNumber, SOID, TransferOrderID, WarehouseID, GIStatus) VALUES (?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, gin.getGinNumber());
                if (gin.getSalesOrder() != null) {
                    ps.setInt(2, gin.getSalesOrder().getSoId());
                } else {
                    ps.setObject(2, null);
                }
                if (gin.getTransferOrder() != null) {
                    ps.setInt(3, gin.getTransferOrder().getToId());
                } else {
                    ps.setObject(3, null);
                }
                ps.setInt(4, gin.getWarehouse().getWarehouseId());
                ps.setString(5, gin.getGiStatus());
                return ps;
            }, keyHolder);
            if (keyHolder.getKey() != null) {
                gin.setGinId(keyHolder.getKey().intValue());
            }
            return gin;
        }
        jdbcTemplate.update(
                "UPDATE GoodsIssueNotes SET SOID = ?, TransferOrderID = ?, WarehouseID = ?, GIStatus = ? WHERE GINID = ?",
                gin.getSalesOrder() != null ? gin.getSalesOrder().getSoId() : null,
                gin.getTransferOrder() != null ? gin.getTransferOrder().getToId() : null,
                gin.getWarehouse().getWarehouseId(),
                gin.getGiStatus(),
                gin.getGinId()
        );
        return gin;
    }

    @Override
    public void updateStatus(Integer ginId, String status) {
        jdbcTemplate.update("UPDATE GoodsIssueNotes SET GIStatus = ? WHERE GINID = ?", status, ginId);
    }

    @Override
    public String findMaxGinNumber(String prefix) {
        return jdbcTemplate.queryForObject(
                "SELECT MAX(GINNumber) FROM GoodsIssueNotes WHERE GINNumber LIKE ?",
                String.class,
                prefix + "%"
        );
    }

    @Override
    public boolean existsBySalesOrderId(Integer soId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM GoodsIssueNotes WHERE SOID = ?",
                Long.class,
                soId
        );
        return count != null && count > 0;
    }
}
