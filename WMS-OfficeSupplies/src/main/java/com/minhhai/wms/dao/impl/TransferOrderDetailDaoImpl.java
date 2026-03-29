package com.minhhai.wms.dao.impl;

import com.minhhai.wms.dao.TransferOrderDetailDao;
import com.minhhai.wms.dao.mapper.TransferOrderDetailRowMapper;
import com.minhhai.wms.entity.TransferOrderDetail;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class TransferOrderDetailDaoImpl implements TransferOrderDetailDao {

    private static final String BASE_SELECT = """
            SELECT d.TODetailID, d.TOID, d.ProductID, d.RequestedQty, d.IssuedQty, d.ReceivedQty, d.UoM,
                   p.SKU, p.ProductName, p.UnitWeight, p.BaseUoM
            FROM TransferOrderDetails d
            JOIN Products p ON d.ProductID = p.ProductID
            """;

    private final JdbcTemplate jdbcTemplate;
    private final TransferOrderDetailRowMapper rowMapper = new TransferOrderDetailRowMapper();

    public TransferOrderDetailDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<TransferOrderDetail> findByTransferOrderId(Integer toId) {
        return jdbcTemplate.query(BASE_SELECT + " WHERE d.TOID = ?", rowMapper, toId);
    }

    @Override
    public TransferOrderDetail save(TransferOrderDetail detail) {
        if (detail.getToDetailId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO TransferOrderDetails (TOID, ProductID, RequestedQty, IssuedQty, ReceivedQty, UoM) VALUES (?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setInt(1, detail.getTransferOrder().getToId());
                ps.setInt(2, detail.getProduct().getProductId());
                ps.setInt(3, detail.getRequestedQty() != null ? detail.getRequestedQty() : 0);
                ps.setInt(4, detail.getIssuedQty() != null ? detail.getIssuedQty() : 0);
                ps.setInt(5, detail.getReceivedQty() != null ? detail.getReceivedQty() : 0);
                ps.setString(6, detail.getUom());
                return ps;
            }, keyHolder);
            if (keyHolder.getKey() != null) {
                detail.setToDetailId(keyHolder.getKey().intValue());
            }
            return detail;
        }
        // TransferOrderDetails are immutable after creation; updates via dedicated methods.
        return detail;
    }

    @Override
    public void updateReceivedQty(Integer toDetailId, Integer receivedQty) {
        jdbcTemplate.update(
                "UPDATE TransferOrderDetails SET ReceivedQty = ? WHERE TODetailID = ?",
                receivedQty, toDetailId);
    }

    @Override
    public void updateIssuedQty(Integer toDetailId, Integer issuedQty) {
        jdbcTemplate.update(
                "UPDATE TransferOrderDetails SET IssuedQty = ? WHERE TODetailID = ?",
                issuedQty, toDetailId);
    }
}
