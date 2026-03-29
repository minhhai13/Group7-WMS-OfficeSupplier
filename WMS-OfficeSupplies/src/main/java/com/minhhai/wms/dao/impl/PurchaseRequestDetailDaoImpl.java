package com.minhhai.wms.dao.impl;

import com.minhhai.wms.dao.PurchaseRequestDetailDao;
import com.minhhai.wms.dao.mapper.PurchaseRequestDetailRowMapper;
import com.minhhai.wms.entity.PurchaseRequestDetail;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class PurchaseRequestDetailDaoImpl implements PurchaseRequestDetailDao {

    private static final String BASE_SELECT = """
            SELECT d.PRDetailID, d.PRID, d.ProductID, d.RequestedQty, d.UoM, d.SODetailID, d.PODetailID,
                   p.SKU, p.ProductName, p.UnitWeight, p.BaseUoM
            FROM PurchaseRequestDetails d
            JOIN Products p ON d.ProductID = p.ProductID
            """;

    private final JdbcTemplate jdbcTemplate;
    private final PurchaseRequestDetailRowMapper rowMapper = new PurchaseRequestDetailRowMapper();

    public PurchaseRequestDetailDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<PurchaseRequestDetail> findByPrId(Integer prId) {
        return jdbcTemplate.query(BASE_SELECT + " WHERE d.PRID = ?", rowMapper, prId);
    }

    @Override
    public PurchaseRequestDetail save(PurchaseRequestDetail detail) {
        if (detail.getPrDetailId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO PurchaseRequestDetails (PRID, ProductID, RequestedQty, UoM, SODetailID, PODetailID) VALUES (?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setInt(1, detail.getPurchaseRequest().getPrId());
                ps.setInt(2, detail.getProduct().getProductId());
                ps.setInt(3, detail.getRequestedQty());
                ps.setString(4, detail.getUom());
                if (detail.getSalesOrderDetail() != null) {
                    ps.setInt(5, detail.getSalesOrderDetail().getSoDetailId());
                } else {
                    ps.setObject(5, null);
                }
                if (detail.getPurchaseOrderDetail() != null) {
                    ps.setInt(6, detail.getPurchaseOrderDetail().getPoDetailId());
                } else {
                    ps.setObject(6, null);
                }
                return ps;
            }, keyHolder);
            if (keyHolder.getKey() != null) {
                detail.setPrDetailId(keyHolder.getKey().intValue());
            }
            return detail;
        }
        jdbcTemplate.update(
                "UPDATE PurchaseRequestDetails SET RequestedQty = ?, UoM = ?, ProductID = ?, SODetailID = ?, PODetailID = ? WHERE PRDetailID = ?",
                detail.getRequestedQty(),
                detail.getUom(),
                detail.getProduct().getProductId(),
                detail.getSalesOrderDetail() != null ? detail.getSalesOrderDetail().getSoDetailId() : null,
                detail.getPurchaseOrderDetail() != null ? detail.getPurchaseOrderDetail().getPoDetailId() : null,
                detail.getPrDetailId()
        );
        return detail;
    }

    @Override
    public void deleteByPrId(Integer prId) {
        jdbcTemplate.update("DELETE FROM PurchaseRequestDetails WHERE PRID = ?", prId);
    }

    @Override
    public void updatePoDetailId(Integer prDetailId, Integer poDetailId) {
        jdbcTemplate.update(
                "UPDATE PurchaseRequestDetails SET PODetailID = ? WHERE PRDetailID = ?",
                poDetailId,
                prDetailId
        );
    }
}
