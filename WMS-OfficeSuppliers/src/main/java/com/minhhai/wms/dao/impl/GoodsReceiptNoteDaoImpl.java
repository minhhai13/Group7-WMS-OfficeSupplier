package com.minhhai.wms.dao.impl;

import com.minhhai.wms.dao.GoodsReceiptNoteDao;
import com.minhhai.wms.dao.mapper.GoodsReceiptNoteRowMapper;
import com.minhhai.wms.entity.GoodsReceiptNote;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class GoodsReceiptNoteDaoImpl implements GoodsReceiptNoteDao {

    private static final String BASE_SELECT = """
            SELECT g.GRNID, g.GRNNumber, g.POID, g.TransferOrderID, g.WarehouseID, g.GRStatus,
                   po.PONumber, po.SupplierID,
                   sup.PartnerName AS SupplierName,
                   t.TONumber, t.SourceWarehouseID, t.DestinationWarehouseID, t.Status AS TOStatus,
                   sw.WarehouseName AS SourceWarehouseName,
                   dw.WarehouseName AS DestinationWarehouseName,
                   w.WarehouseName
            FROM GoodsReceiptNotes g
            JOIN Warehouses w ON g.WarehouseID = w.WarehouseID
            LEFT JOIN PurchaseOrders po ON g.POID = po.POID
            LEFT JOIN Partners sup ON po.SupplierID = sup.PartnerID
            LEFT JOIN TransferOrders t ON g.TransferOrderID = t.TOID
            LEFT JOIN Warehouses sw ON t.SourceWarehouseID = sw.WarehouseID
            LEFT JOIN Warehouses dw ON t.DestinationWarehouseID = dw.WarehouseID
            """;

    private final JdbcTemplate jdbcTemplate;
    private final GoodsReceiptNoteRowMapper rowMapper = new GoodsReceiptNoteRowMapper();

    public GoodsReceiptNoteDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<GoodsReceiptNote> findByWarehouseId(Integer warehouseId) {
        return jdbcTemplate.query(BASE_SELECT + " WHERE g.WarehouseID = ?", rowMapper, warehouseId);
    }

    @Override
    public List<GoodsReceiptNote> findByWarehouseIdAndStatus(Integer warehouseId, String status) {
        return jdbcTemplate.query(
                BASE_SELECT + " WHERE g.WarehouseID = ? AND g.GRStatus = ?",
                rowMapper,
                warehouseId,
                status
        );
    }

    @Override
    public Optional<GoodsReceiptNote> findById(Integer grnId) {
        List<GoodsReceiptNote> list = jdbcTemplate.query(BASE_SELECT + " WHERE g.GRNID = ?", rowMapper, grnId);
        return list.stream().findFirst();
    }

    @Override
    public GoodsReceiptNote save(GoodsReceiptNote grn) {
        if (grn.getGrnId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO GoodsReceiptNotes (GRNNumber, POID, TransferOrderID, WarehouseID, GRStatus) VALUES (?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, grn.getGrnNumber());
                if (grn.getPurchaseOrder() != null) {
                    ps.setInt(2, grn.getPurchaseOrder().getPoId());
                } else {
                    ps.setObject(2, null);
                }
                if (grn.getTransferOrder() != null) {
                    ps.setInt(3, grn.getTransferOrder().getToId());
                } else {
                    ps.setObject(3, null);
                }
                ps.setInt(4, grn.getWarehouse().getWarehouseId());
                ps.setString(5, grn.getGrStatus());
                return ps;
            }, keyHolder);
            if (keyHolder.getKey() != null) {
                grn.setGrnId(keyHolder.getKey().intValue());
            }
            return grn;
        }
        jdbcTemplate.update(
                "UPDATE GoodsReceiptNotes SET POID = ?, TransferOrderID = ?, WarehouseID = ?, GRStatus = ? WHERE GRNID = ?",
                grn.getPurchaseOrder() != null ? grn.getPurchaseOrder().getPoId() : null,
                grn.getTransferOrder() != null ? grn.getTransferOrder().getToId() : null,
                grn.getWarehouse().getWarehouseId(),
                grn.getGrStatus(),
                grn.getGrnId()
        );
        return grn;
    }

    @Override
    public void updateStatus(Integer grnId, String status) {
        jdbcTemplate.update("UPDATE GoodsReceiptNotes SET GRStatus = ? WHERE GRNID = ?", status, grnId);
    }

    @Override
    public String findMaxGrnNumber(String prefix) {
        return jdbcTemplate.queryForObject(
                "SELECT MAX(GRNNumber) FROM GoodsReceiptNotes WHERE GRNNumber LIKE ?",
                String.class,
                prefix + "%"
        );
    }
}
