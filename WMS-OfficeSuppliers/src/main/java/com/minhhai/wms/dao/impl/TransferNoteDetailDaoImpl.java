package com.minhhai.wms.dao.impl;

import com.minhhai.wms.dao.TransferNoteDetailDao;
import com.minhhai.wms.dao.mapper.TransferNoteDetailRowMapper;
import com.minhhai.wms.entity.TransferNoteDetail;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class TransferNoteDetailDaoImpl implements TransferNoteDetailDao {

    private static final String BASE_SELECT = """
            SELECT d.TNDetailID, d.TNID, d.ProductID, d.BatchNumber, d.FromBinID, d.ToBinID, d.Quantity, d.UoM,
                   p.SKU, p.ProductName, p.UnitWeight, p.BaseUoM,
                   fb.BinLocation AS FromBinLocation,
                   tb.BinLocation AS ToBinLocation
            FROM TransferNoteDetails d
            JOIN Products p ON d.ProductID = p.ProductID
            JOIN Bins fb ON d.FromBinID = fb.BinID
            JOIN Bins tb ON d.ToBinID = tb.BinID
            """;

    private final JdbcTemplate jdbcTemplate;
    private final TransferNoteDetailRowMapper rowMapper = new TransferNoteDetailRowMapper();

    public TransferNoteDetailDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<TransferNoteDetail> findByTnId(Integer tnId) {
        return jdbcTemplate.query(BASE_SELECT + " WHERE d.TNID = ?", rowMapper, tnId);
    }

    @Override
    public TransferNoteDetail save(TransferNoteDetail detail) {
        if (detail.getTnDetailId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO TransferNoteDetails (TNID, ProductID, BatchNumber, FromBinID, ToBinID, Quantity, UoM) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setInt(1, detail.getTransferNote().getTnId());
                ps.setInt(2, detail.getProduct().getProductId());
                ps.setString(3, detail.getBatchNumber());
                ps.setInt(4, detail.getFromBin().getBinId());
                ps.setInt(5, detail.getToBin().getBinId());
                ps.setInt(6, detail.getQuantity());
                ps.setString(7, detail.getUom());
                return ps;
            }, keyHolder);
            if (keyHolder.getKey() != null) {
                detail.setTnDetailId(keyHolder.getKey().intValue());
            }
            return detail;
        }
        // TransferNoteDetails are immutable after creation; no UPDATE needed.
        return detail;
    }

    @Override
    public void deleteByTnId(Integer tnId) {
        jdbcTemplate.update("DELETE FROM TransferNoteDetails WHERE TNID = ?", tnId);
    }
}
