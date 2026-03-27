package com.minhhai.wms.dao.impl;

import com.minhhai.wms.dao.GoodsIssueDetailDao;
import com.minhhai.wms.dao.mapper.GoodsIssueDetailRowMapper;
import com.minhhai.wms.entity.GoodsIssueDetail;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class GoodsIssueDetailDaoImpl implements GoodsIssueDetailDao {

    private static final String BASE_SELECT = """
            SELECT d.GIDetailID, d.GINID, d.SODetailID, d.TODetailID, d.PlannedQty, d.ProductID, d.IssuedQty,
                   d.UoM, d.BatchNumber, d.BinID,
                   p.SKU, p.ProductName, p.UnitWeight, p.BaseUoM,
                   b.BinLocation,
                   sod.OrderedQty AS SODetailOrderedQty, sod.IssuedQty AS SODetailIssuedQty, sod.UoM AS SODetailUoM,
                   tod.RequestedQty AS TODetailRequestedQty, tod.IssuedQty AS TODetailIssuedQty, tod.ReceivedQty AS TODetailReceivedQty, tod.UoM AS TODetailUoM
            FROM GoodsIssueDetails d
            JOIN Products p ON d.ProductID = p.ProductID
            JOIN Bins b ON d.BinID = b.BinID
            LEFT JOIN SalesOrderDetails sod ON d.SODetailID = sod.SODetailID
            LEFT JOIN TransferOrderDetails tod ON d.TODetailID = tod.TODetailID
            """;

    private final JdbcTemplate jdbcTemplate;
    private final GoodsIssueDetailRowMapper rowMapper = new GoodsIssueDetailRowMapper();

    public GoodsIssueDetailDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<GoodsIssueDetail> findByGinId(Integer ginId) {
        return jdbcTemplate.query(BASE_SELECT + " WHERE d.GINID = ?", rowMapper, ginId);
    }

    @Override
    public GoodsIssueDetail save(GoodsIssueDetail detail) {
        if (detail.getGiDetailId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO GoodsIssueDetails (GINID, SODetailID, TODetailID, PlannedQty, ProductID, IssuedQty, UoM, BatchNumber, BinID) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setInt(1, detail.getGoodsIssueNote().getGinId());
                if (detail.getSalesOrderDetail() != null) {
                    ps.setInt(2, detail.getSalesOrderDetail().getSoDetailId());
                } else {
                    ps.setObject(2, null);
                }
                if (detail.getTransferOrderDetail() != null) {
                    ps.setInt(3, detail.getTransferOrderDetail().getToDetailId());
                } else {
                    ps.setObject(3, null);
                }
                ps.setInt(4, detail.getPlannedQty() != null ? detail.getPlannedQty() : 0);
                ps.setInt(5, detail.getProduct().getProductId());
                ps.setInt(6, detail.getIssuedQty() != null ? detail.getIssuedQty() : 0);
                ps.setString(7, detail.getUom());
                ps.setString(8, detail.getBatchNumber());
                ps.setInt(9, detail.getBin().getBinId());
                return ps;
            }, keyHolder);
            if (keyHolder.getKey() != null) {
                detail.setGiDetailId(keyHolder.getKey().intValue());
            }
            return detail;
        }
        jdbcTemplate.update(
                "UPDATE GoodsIssueDetails SET PlannedQty = ?, IssuedQty = ?, UoM = ?, BatchNumber = ?, BinID = ?, SODetailID = ?, TODetailID = ?, ProductID = ? WHERE GIDetailID = ?",
                detail.getPlannedQty(),
                detail.getIssuedQty(),
                detail.getUom(),
                detail.getBatchNumber(),
                detail.getBin().getBinId(),
                detail.getSalesOrderDetail() != null ? detail.getSalesOrderDetail().getSoDetailId() : null,
                detail.getTransferOrderDetail() != null ? detail.getTransferOrderDetail().getToDetailId() : null,
                detail.getProduct().getProductId(),
                detail.getGiDetailId()
        );
        return detail;
    }

    @Override
    public void updateIssuedQty(Integer giDetailId, Integer issuedQty) {
        jdbcTemplate.update(
                "UPDATE GoodsIssueDetails SET IssuedQty = ? WHERE GIDetailID = ?",
                issuedQty,
                giDetailId
        );
    }
}
