package com.minhhai.wms.dao.impl;

import com.minhhai.wms.dao.PurchaseOrderDao;
import com.minhhai.wms.dao.mapper.PurchaseOrderRowMapper;
import com.minhhai.wms.entity.PurchaseOrder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class PurchaseOrderDaoImpl implements PurchaseOrderDao {

    private static final String BASE_SELECT = """
            SELECT po.POID, po.PONumber, po.WarehouseID, po.SupplierID, po.POStatus, po.RejectReason,
                   w.WarehouseCode, w.WarehouseName, w.Address, w.IsActive AS WarehouseIsActive,
                   s.PartnerName, s.PartnerType, s.ContactPerson, s.PhoneNumber, s.IsActive AS SupplierIsActive
            FROM PurchaseOrders po
            JOIN Warehouses w ON po.WarehouseID = w.WarehouseID
            JOIN Partners s ON po.SupplierID = s.PartnerID
            """;

    private final JdbcTemplate jdbcTemplate;
    private final PurchaseOrderRowMapper rowMapper = new PurchaseOrderRowMapper();

    public PurchaseOrderDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<PurchaseOrder> findByWarehouseId(Integer warehouseId) {
        return jdbcTemplate.query(BASE_SELECT + " WHERE po.WarehouseID = ?", rowMapper, warehouseId);
    }

    @Override
    public List<PurchaseOrder> findByWarehouseIdAndStatus(Integer warehouseId, String status) {
        return jdbcTemplate.query(
                BASE_SELECT + " WHERE po.WarehouseID = ? AND po.POStatus = ?",
                rowMapper,
                warehouseId,
                status
        );
    }

    @Override
    public List<PurchaseOrder> findByWarehouseIdAndSupplierId(Integer warehouseId, Integer supplierId) {
        return jdbcTemplate.query(
                BASE_SELECT + " WHERE po.WarehouseID = ? AND po.SupplierID = ?",
                rowMapper,
                warehouseId,
                supplierId
        );
    }

    @Override
    public List<PurchaseOrder> findByWarehouseIdAndStatusAndSupplierId(Integer warehouseId, String status, Integer supplierId) {
        return jdbcTemplate.query(
                BASE_SELECT + " WHERE po.WarehouseID = ? AND po.POStatus = ? AND po.SupplierID = ?",
                rowMapper,
                warehouseId,
                status,
                supplierId
        );
    }

    @Override
    public Optional<PurchaseOrder> findById(Integer poId) {
        List<PurchaseOrder> list = jdbcTemplate.query(BASE_SELECT + " WHERE po.POID = ?", rowMapper, poId);
        return list.stream().findFirst();
    }

    @Override
    public PurchaseOrder save(PurchaseOrder purchaseOrder) {
        if (purchaseOrder.getPoId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO PurchaseOrders (PONumber, WarehouseID, SupplierID, RejectReason, POStatus) VALUES (?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, purchaseOrder.getPoNumber());
                ps.setInt(2, purchaseOrder.getWarehouse().getWarehouseId());
                ps.setInt(3, purchaseOrder.getSupplier().getPartnerId());
                ps.setString(4, purchaseOrder.getRejectReason());
                ps.setString(5, purchaseOrder.getPoStatus());
                return ps;
            }, keyHolder);
            if (keyHolder.getKey() != null) {
                purchaseOrder.setPoId(keyHolder.getKey().intValue());
            }
            return purchaseOrder;
        }
        jdbcTemplate.update(
                "UPDATE PurchaseOrders SET WarehouseID = ?, SupplierID = ?, RejectReason = ?, POStatus = ? WHERE POID = ?",
                purchaseOrder.getWarehouse().getWarehouseId(),
                purchaseOrder.getSupplier().getPartnerId(),
                purchaseOrder.getRejectReason(),
                purchaseOrder.getPoStatus(),
                purchaseOrder.getPoId()
        );
        return purchaseOrder;
    }

    @Override
    public void deleteById(Integer poId) {
        jdbcTemplate.update("DELETE FROM PurchaseOrders WHERE POID = ?", poId);
    }

    @Override
    public String findMaxPoNumber(String prefix) {
        return jdbcTemplate.queryForObject(
                "SELECT MAX(PONumber) FROM PurchaseOrders WHERE PONumber LIKE ?",
                String.class,
                prefix + "%"
        );
    }
}
