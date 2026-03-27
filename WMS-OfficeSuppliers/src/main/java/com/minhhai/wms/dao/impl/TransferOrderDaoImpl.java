package com.minhhai.wms.dao.impl;

import com.minhhai.wms.dao.TransferOrderDao;
import com.minhhai.wms.dao.mapper.TransferOrderRowMapper;
import com.minhhai.wms.entity.TransferOrder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class TransferOrderDaoImpl implements TransferOrderDao {

    private static final String BASE_SELECT = """
            SELECT t.TOID, t.TONumber, t.SourceWarehouseID, t.DestinationWarehouseID,
                   t.Status, t.CreatedBy, t.RejectReason,
                   sw.WarehouseName AS SourceWarehouseName,
                   dw.WarehouseName AS DestinationWarehouseName,
                   u.FullName AS CreatorFullName
            FROM TransferOrders t
            JOIN Warehouses sw ON t.SourceWarehouseID = sw.WarehouseID
            JOIN Warehouses dw ON t.DestinationWarehouseID = dw.WarehouseID
            JOIN Users u ON t.CreatedBy = u.UserID
            """;

    private final JdbcTemplate jdbcTemplate;
    private final TransferOrderRowMapper rowMapper = new TransferOrderRowMapper();

    public TransferOrderDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ── Destination warehouse perspective (outgoing view) ────────────────────

    @Override
    public List<TransferOrder> findByDestinationWarehouseId(Integer destWarehouseId) {
        return jdbcTemplate.query(
                BASE_SELECT + " WHERE t.DestinationWarehouseID = ? ORDER BY t.TOID DESC",
                rowMapper, destWarehouseId);
    }

    @Override
    public List<TransferOrder> findByDestinationWarehouseIdAndStatus(Integer destWarehouseId, String status) {
        return jdbcTemplate.query(
                BASE_SELECT + " WHERE t.DestinationWarehouseID = ? AND t.Status = ? ORDER BY t.TOID DESC",
                rowMapper, destWarehouseId, status);
    }

    @Override
    public List<TransferOrder> findBySourceAndDestinationWarehouseId(Integer sourceWarehouseId, Integer destWarehouseId) {
        return jdbcTemplate.query(
                BASE_SELECT + " WHERE t.SourceWarehouseID = ? AND t.DestinationWarehouseID = ? ORDER BY t.TOID DESC",
                rowMapper, sourceWarehouseId, destWarehouseId);
    }

    @Override
    public List<TransferOrder> findBySourceAndDestinationWarehouseIdAndStatus(Integer sourceWarehouseId, String status, Integer destWarehouseId) {
        return jdbcTemplate.query(
                BASE_SELECT + " WHERE t.SourceWarehouseID = ? AND t.Status = ? AND t.DestinationWarehouseID = ? ORDER BY t.TOID DESC",
                rowMapper, sourceWarehouseId, status, destWarehouseId);
    }

    // ── Source warehouse perspective (incoming view) ──────────────────────────

    @Override
    public List<TransferOrder> findBySourceWarehouseId(Integer sourceWarehouseId) {
        return jdbcTemplate.query(
                BASE_SELECT + " WHERE t.SourceWarehouseID = ? ORDER BY t.TOID DESC",
                rowMapper, sourceWarehouseId);
    }

    @Override
    public List<TransferOrder> findBySourceWarehouseIdAndStatus(Integer sourceWarehouseId, String status) {
        return jdbcTemplate.query(
                BASE_SELECT + " WHERE t.SourceWarehouseID = ? AND t.Status = ? ORDER BY t.TOID DESC",
                rowMapper, sourceWarehouseId, status);
    }

    @Override
    public Optional<TransferOrder> findById(Integer toId) {
        List<TransferOrder> list = jdbcTemplate.query(
                BASE_SELECT + " WHERE t.TOID = ?", rowMapper, toId);
        return list.stream().findFirst();
    }

    // ── Persist ──────────────────────────────────────────────────────────────

    @Override
    public TransferOrder save(TransferOrder to) {
        if (to.getToId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO TransferOrders (TONumber, SourceWarehouseID, DestinationWarehouseID, Status, CreatedBy, RejectReason) VALUES (?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, to.getToNumber());
                ps.setInt(2, to.getSourceWarehouse().getWarehouseId());
                ps.setInt(3, to.getDestinationWarehouse().getWarehouseId());
                ps.setString(4, to.getStatus() != null ? to.getStatus() : "Draft");
                ps.setInt(5, to.getCreatedBy().getUserId());
                ps.setString(6, to.getRejectReason());
                return ps;
            }, keyHolder);
            if (keyHolder.getKey() != null) {
                to.setToId(keyHolder.getKey().intValue());
            }
            return to;
        }
        jdbcTemplate.update(
                "UPDATE TransferOrders SET Status = ?, RejectReason = ? WHERE TOID = ?",
                to.getStatus(),
                to.getRejectReason(),
                to.getToId()
        );
        return to;
    }

    @Override
    public void deleteById(Integer toId) {
        jdbcTemplate.update("DELETE FROM TransferOrders WHERE TOID = ?", toId);
    }

    @Override
    public void updateStatus(Integer toId, String status) {
        jdbcTemplate.update("UPDATE TransferOrders SET Status = ? WHERE TOID = ?", status, toId);
    }

    @Override
    public void updateStatusAndRejectReason(Integer toId, String status, String rejectReason) {
        jdbcTemplate.update(
                "UPDATE TransferOrders SET Status = ?, RejectReason = ? WHERE TOID = ?",
                status, rejectReason, toId);
    }

    @Override
    public String findMaxToNumber(String prefix) {
        return jdbcTemplate.queryForObject(
                "SELECT MAX(TONumber) FROM TransferOrders WHERE TONumber LIKE ?",
                String.class,
                prefix + "%"
        );
    }
}
