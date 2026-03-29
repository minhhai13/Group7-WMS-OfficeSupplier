package com.minhhai.wms.dao.impl;

import com.minhhai.wms.dao.SalesOrderDao;
import com.minhhai.wms.dao.mapper.SalesOrderRowMapper;
import com.minhhai.wms.entity.SalesOrder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class SalesOrderDaoImpl implements SalesOrderDao {

    private static final String BASE_SELECT = """
            SELECT so.SOID, so.SONumber, so.WarehouseID, so.CustomerID, so.RejectReason, so.SOStatus,
                   w.WarehouseCode, w.WarehouseName, w.Address, w.IsActive AS WarehouseIsActive,
                   c.PartnerName, c.PartnerType, c.ContactPerson, c.PhoneNumber, c.IsActive AS CustomerIsActive
            FROM SalesOrders so
            JOIN Warehouses w ON so.WarehouseID = w.WarehouseID
            JOIN Partners c ON so.CustomerID = c.PartnerID
            """;

    private final JdbcTemplate jdbcTemplate;
    private final SalesOrderRowMapper rowMapper = new SalesOrderRowMapper();

    public SalesOrderDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<SalesOrder> findByWarehouseId(Integer warehouseId) {
        return jdbcTemplate.query(BASE_SELECT + " WHERE so.WarehouseID = ?", rowMapper, warehouseId);
    }

    @Override
    public List<SalesOrder> findByWarehouseIdAndStatus(Integer warehouseId, String status) {
        return jdbcTemplate.query(
                BASE_SELECT + " WHERE so.WarehouseID = ? AND so.SOStatus = ?",
                rowMapper,
                warehouseId,
                status
        );
    }

    @Override
    public List<SalesOrder> findByWarehouseIdAndCustomerId(Integer warehouseId, Integer customerId) {
        return jdbcTemplate.query(
                BASE_SELECT + " WHERE so.WarehouseID = ? AND so.CustomerID = ?",
                rowMapper,
                warehouseId,
                customerId
        );
    }

    @Override
    public List<SalesOrder> findByWarehouseIdAndStatusAndCustomerId(Integer warehouseId, String status, Integer customerId) {
        return jdbcTemplate.query(
                BASE_SELECT + " WHERE so.WarehouseID = ? AND so.SOStatus = ? AND so.CustomerID = ?",
                rowMapper,
                warehouseId,
                status,
                customerId
        );
    }

    @Override
    public Optional<SalesOrder> findById(Integer soId) {
        List<SalesOrder> list = jdbcTemplate.query(BASE_SELECT + " WHERE so.SOID = ?", rowMapper, soId);
        return list.stream().findFirst();
    }

    @Override
    public SalesOrder save(SalesOrder salesOrder) {
        if (salesOrder.getSoId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO SalesOrders (SONumber, WarehouseID, CustomerID, RejectReason, SOStatus) VALUES (?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, salesOrder.getSoNumber());
                ps.setInt(2, salesOrder.getWarehouse().getWarehouseId());
                ps.setInt(3, salesOrder.getCustomer().getPartnerId());
                ps.setString(4, salesOrder.getRejectReason());
                ps.setString(5, salesOrder.getSoStatus());
                return ps;
            }, keyHolder);
            if (keyHolder.getKey() != null) {
                salesOrder.setSoId(keyHolder.getKey().intValue());
            }
            return salesOrder;
        }
        jdbcTemplate.update(
                "UPDATE SalesOrders SET WarehouseID = ?, CustomerID = ?, RejectReason = ?, SOStatus = ? WHERE SOID = ?",
                salesOrder.getWarehouse().getWarehouseId(),
                salesOrder.getCustomer().getPartnerId(),
                salesOrder.getRejectReason(),
                salesOrder.getSoStatus(),
                salesOrder.getSoId()
        );
        return salesOrder;
    }

    @Override
    public void deleteById(Integer soId) {
        jdbcTemplate.update("DELETE FROM SalesOrders WHERE SOID = ?", soId);
    }

    @Override
    public String findMaxSoNumber(String prefix) {
        return jdbcTemplate.queryForObject(
                "SELECT MAX(SONumber) FROM SalesOrders WHERE SONumber LIKE ?",
                String.class,
                prefix + "%"
        );
    }
}
