package com.minhhai.wms.dao.impl;

import com.minhhai.wms.dao.TransferNoteDao;
import com.minhhai.wms.dao.mapper.TransferNoteRowMapper;
import com.minhhai.wms.entity.TransferNote;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class TransferNoteDaoImpl implements TransferNoteDao {

    private static final String BASE_SELECT = """
            SELECT tn.TNID, tn.TNNumber, tn.WarehouseID, tn.Status, tn.CreatedBy, tn.CreatedAt,
                   w.WarehouseCode, w.WarehouseName,
                   u.FullName AS CreatorFullName
            FROM TransferNotes tn
            JOIN Warehouses w ON tn.WarehouseID = w.WarehouseID
            JOIN Users u ON tn.CreatedBy = u.UserID
            """;

    private final JdbcTemplate jdbcTemplate;
    private final TransferNoteRowMapper rowMapper = new TransferNoteRowMapper();

    public TransferNoteDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<TransferNote> findByWarehouseId(Integer warehouseId) {
        return jdbcTemplate.query(BASE_SELECT + " WHERE tn.WarehouseID = ? ORDER BY tn.CreatedAt DESC",
                rowMapper, warehouseId);
    }

    @Override
    public Optional<TransferNote> findById(Integer tnId) {
        List<TransferNote> list = jdbcTemplate.query(
                BASE_SELECT + " WHERE tn.TNID = ?", rowMapper, tnId);
        return list.stream().findFirst();
    }

    @Override
    public TransferNote save(TransferNote tn) {
        if (tn.getTnId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO TransferNotes (TNNumber, WarehouseID, Status, CreatedBy, CreatedAt) VALUES (?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, tn.getTnNumber());
                ps.setInt(2, tn.getWarehouse().getWarehouseId());
                ps.setString(3, tn.getStatus() != null ? tn.getStatus() : "Approved");
                ps.setInt(4, tn.getCreatedBy().getUserId());
                ps.setTimestamp(5, tn.getCreatedAt() != null
                        ? Timestamp.valueOf(tn.getCreatedAt()) : new Timestamp(System.currentTimeMillis()));
                return ps;
            }, keyHolder);
            if (keyHolder.getKey() != null) {
                tn.setTnId(keyHolder.getKey().intValue());
            }
            return tn;
        }
        jdbcTemplate.update(
                "UPDATE TransferNotes SET Status = ? WHERE TNID = ?",
                tn.getStatus(),
                tn.getTnId()
        );
        return tn;
    }

    @Override
    public void updateStatus(Integer tnId, String status) {
        jdbcTemplate.update("UPDATE TransferNotes SET Status = ? WHERE TNID = ?", status, tnId);
    }

    @Override
    public String findMaxTnNumber(String prefix) {
        return jdbcTemplate.queryForObject(
                "SELECT MAX(TNNumber) FROM TransferNotes WHERE TNNumber LIKE ?",
                String.class,
                prefix + "%"
        );
    }
}
