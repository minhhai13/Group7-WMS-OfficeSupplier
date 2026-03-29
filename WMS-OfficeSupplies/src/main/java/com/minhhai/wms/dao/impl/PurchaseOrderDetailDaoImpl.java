package com.minhhai.wms.dao.impl;

import com.minhhai.wms.dao.PurchaseOrderDetailDao;
import com.minhhai.wms.dao.mapper.PurchaseOrderDetailRowMapper;
import com.minhhai.wms.entity.PurchaseOrderDetail;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class PurchaseOrderDetailDaoImpl implements PurchaseOrderDetailDao {

    private static final String BASE_SELECT = """
            SELECT d.PODetailID, d.POID, d.ProductID, d.OrderedQty, d.ReceivedQty, d.UoM,
                   p.SKU, p.ProductName, p.UnitWeight, p.BaseUoM, p.MinStockLevel, p.IsActive AS ProductIsActive
            FROM PurchaseOrderDetails d
            JOIN Products p ON d.ProductID = p.ProductID
            """;

    private final JdbcTemplate jdbcTemplate;
    private final PurchaseOrderDetailRowMapper rowMapper = new PurchaseOrderDetailRowMapper();

    public PurchaseOrderDetailDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<PurchaseOrderDetail> findByPoId(Integer poId) {
        return jdbcTemplate.query(BASE_SELECT + " WHERE d.POID = ?", rowMapper, poId);
    }

    @Override
    public PurchaseOrderDetail save(PurchaseOrderDetail detail) {
        if (detail.getPoDetailId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO PurchaseOrderDetails (POID, ProductID, OrderedQty, ReceivedQty, UoM) VALUES (?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setInt(1, detail.getPurchaseOrder().getPoId());
                ps.setInt(2, detail.getProduct().getProductId());
                ps.setInt(3, detail.getOrderedQty());
                ps.setInt(4, detail.getReceivedQty() != null ? detail.getReceivedQty() : 0);
                ps.setString(5, detail.getUom());
                return ps;
            }, keyHolder);
            if (keyHolder.getKey() != null) {
                detail.setPoDetailId(keyHolder.getKey().intValue());
            }
            return detail;
        }
        jdbcTemplate.update(
                "UPDATE PurchaseOrderDetails SET OrderedQty = ?, ReceivedQty = ?, UoM = ?, ProductID = ? WHERE PODetailID = ?",
                detail.getOrderedQty(),
                detail.getReceivedQty(),
                detail.getUom(),
                detail.getProduct().getProductId(),
                detail.getPoDetailId()
        );
        return detail;
    }

    @Override
    public void deleteByPoId(Integer poId) {
        jdbcTemplate.update("DELETE FROM PurchaseOrderDetails WHERE POID = ?", poId);
    }

    @Override
    public void updateReceivedQty(Integer poDetailId, Integer receivedQty) {
        jdbcTemplate.update(
                "UPDATE PurchaseOrderDetails SET ReceivedQty = ? WHERE PODetailID = ?",
                receivedQty,
                poDetailId
        );
    }
}
