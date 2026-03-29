package com.minhhai.wms.dao.impl;

import com.minhhai.wms.dao.SalesOrderDetailDao;
import com.minhhai.wms.dao.mapper.SalesOrderDetailRowMapper;
import com.minhhai.wms.entity.SalesOrderDetail;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class SalesOrderDetailDaoImpl implements SalesOrderDetailDao {

    private static final String BASE_SELECT = """
            SELECT d.SODetailID, d.SOID, d.ProductID, d.OrderedQty, d.IssuedQty, d.UoM,
                   p.SKU, p.ProductName, p.UnitWeight, p.BaseUoM, p.MinStockLevel, p.IsActive AS ProductIsActive
            FROM SalesOrderDetails d
            JOIN Products p ON d.ProductID = p.ProductID
            """;

    private final JdbcTemplate jdbcTemplate;
    private final SalesOrderDetailRowMapper rowMapper = new SalesOrderDetailRowMapper();

    public SalesOrderDetailDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<SalesOrderDetail> findBySoId(Integer soId) {
        return jdbcTemplate.query(BASE_SELECT + " WHERE d.SOID = ?", rowMapper, soId);
    }

    @Override
    public SalesOrderDetail save(SalesOrderDetail detail) {
        if (detail.getSoDetailId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO SalesOrderDetails (SOID, ProductID, OrderedQty, IssuedQty, UoM) VALUES (?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setInt(1, detail.getSalesOrder().getSoId());
                ps.setInt(2, detail.getProduct().getProductId());
                ps.setInt(3, detail.getOrderedQty());
                ps.setInt(4, detail.getIssuedQty() != null ? detail.getIssuedQty() : 0);
                ps.setString(5, detail.getUom());
                return ps;
            }, keyHolder);
            if (keyHolder.getKey() != null) {
                detail.setSoDetailId(keyHolder.getKey().intValue());
            }
            return detail;
        }
        jdbcTemplate.update(
                "UPDATE SalesOrderDetails SET OrderedQty = ?, IssuedQty = ?, UoM = ?, ProductID = ? WHERE SODetailID = ?",
                detail.getOrderedQty(),
                detail.getIssuedQty(),
                detail.getUom(),
                detail.getProduct().getProductId(),
                detail.getSoDetailId()
        );
        return detail;
    }

    @Override
    public void deleteBySoId(Integer soId) {
        jdbcTemplate.update("DELETE FROM SalesOrderDetails WHERE SOID = ?", soId);
    }

    @Override
    public void updateIssuedQty(Integer soDetailId, Integer issuedQty) {
        jdbcTemplate.update(
                "UPDATE SalesOrderDetails SET IssuedQty = ? WHERE SODetailID = ?",
                issuedQty,
                soDetailId
        );
    }
}
