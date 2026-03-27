package com.minhhai.wms.dao.impl;

import com.minhhai.wms.dao.GoodsReceiptDetailDao;
import com.minhhai.wms.dao.mapper.GoodsReceiptDetailRowMapper;
import com.minhhai.wms.entity.GoodsReceiptDetail;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class GoodsReceiptDetailDaoImpl implements GoodsReceiptDetailDao {

    private static final String BASE_SELECT = """
            SELECT d.GRDetailID, d.GRNID, d.PODetailID, d.TODetailID, d.ProductID, d.ReceivedQty, d.ExpectedQty,
                   d.UoM, d.BatchNumber, d.BinID,
                   p.SKU, p.ProductName, p.UnitWeight, p.BaseUoM,
                   b.BinLocation,
                   pod.OrderedQty AS PODetailOrderedQty, pod.ReceivedQty AS PODetailReceivedQty, pod.UoM AS PODetailUoM,
                   tod.RequestedQty AS TODetailRequestedQty, tod.IssuedQty AS TODetailIssuedQty, tod.ReceivedQty AS TODetailReceivedQty, tod.UoM AS TODetailUoM
            FROM GoodsReceiptDetails d
            JOIN Products p ON d.ProductID = p.ProductID
            JOIN Bins b ON d.BinID = b.BinID
            LEFT JOIN PurchaseOrderDetails pod ON d.PODetailID = pod.PODetailID
            LEFT JOIN TransferOrderDetails tod ON d.TODetailID = tod.TODetailID
            """;

    private final JdbcTemplate jdbcTemplate;
    private final GoodsReceiptDetailRowMapper rowMapper = new GoodsReceiptDetailRowMapper();

    public GoodsReceiptDetailDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<GoodsReceiptDetail> findByGrnId(Integer grnId) {
        return jdbcTemplate.query(BASE_SELECT + " WHERE d.GRNID = ?", rowMapper, grnId);
    }

    @Override
    public List<GoodsReceiptDetail> findDraftByBinId(Integer binId) {
        String sql = """
                SELECT d.GRDetailID, d.GRNID, d.PODetailID, d.TODetailID, d.ProductID, d.ReceivedQty, d.ExpectedQty,
                       d.UoM, d.BatchNumber, d.BinID,
                       p.SKU, p.ProductName, p.UnitWeight, p.BaseUoM,
                       b.BinLocation,
                       pod.OrderedQty AS PODetailOrderedQty, pod.ReceivedQty AS PODetailReceivedQty, pod.UoM AS PODetailUoM,
                       tod.RequestedQty AS TODetailRequestedQty, tod.IssuedQty AS TODetailIssuedQty, tod.ReceivedQty AS TODetailReceivedQty, tod.UoM AS TODetailUoM
                FROM GoodsReceiptDetails d
                JOIN GoodsReceiptNotes g ON d.GRNID = g.GRNID
                JOIN Products p ON d.ProductID = p.ProductID
                JOIN Bins b ON d.BinID = b.BinID
                LEFT JOIN PurchaseOrderDetails pod ON d.PODetailID = pod.PODetailID
                LEFT JOIN TransferOrderDetails tod ON d.TODetailID = tod.TODetailID
                WHERE d.BinID = ? AND g.GRStatus = 'Draft'
                """;
        return jdbcTemplate.query(sql, rowMapper, binId);
    }

    @Override
    public GoodsReceiptDetail save(GoodsReceiptDetail detail) {
        if (detail.getGrDetailId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO GoodsReceiptDetails (GRNID, PODetailID, TODetailID, ProductID, ReceivedQty, ExpectedQty, UoM, BatchNumber, BinID) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setInt(1, detail.getGoodsReceiptNote().getGrnId());
                if (detail.getPurchaseOrderDetail() != null) {
                    ps.setInt(2, detail.getPurchaseOrderDetail().getPoDetailId());
                } else {
                    ps.setObject(2, null);
                }
                if (detail.getTransferOrderDetail() != null) {
                    ps.setInt(3, detail.getTransferOrderDetail().getToDetailId());
                } else {
                    ps.setObject(3, null);
                }
                ps.setInt(4, detail.getProduct().getProductId());
                ps.setInt(5, detail.getReceivedQty() != null ? detail.getReceivedQty() : 0);
                ps.setInt(6, detail.getExpectedQty() != null ? detail.getExpectedQty() : 0);
                ps.setString(7, detail.getUom());
                ps.setString(8, detail.getBatchNumber());
                ps.setInt(9, detail.getBin().getBinId());
                return ps;
            }, keyHolder);
            if (keyHolder.getKey() != null) {
                detail.setGrDetailId(keyHolder.getKey().intValue());
            }
            return detail;
        }
        jdbcTemplate.update(
                "UPDATE GoodsReceiptDetails SET ReceivedQty = ?, ExpectedQty = ?, UoM = ?, BatchNumber = ?, BinID = ?, PODetailID = ?, TODetailID = ?, ProductID = ? WHERE GRDetailID = ?",
                detail.getReceivedQty(),
                detail.getExpectedQty(),
                detail.getUom(),
                detail.getBatchNumber(),
                detail.getBin().getBinId(),
                detail.getPurchaseOrderDetail() != null ? detail.getPurchaseOrderDetail().getPoDetailId() : null,
                detail.getTransferOrderDetail() != null ? detail.getTransferOrderDetail().getToDetailId() : null,
                detail.getProduct().getProductId(),
                detail.getGrDetailId()
        );
        return detail;
    }

    @Override
    public void updateReceivedQty(Integer grDetailId, Integer receivedQty) {
        jdbcTemplate.update(
                "UPDATE GoodsReceiptDetails SET ReceivedQty = ? WHERE GRDetailID = ?",
                receivedQty,
                grDetailId
        );
    }
}
