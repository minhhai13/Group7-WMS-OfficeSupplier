package com.minhhai.wms.dao.impl;

import com.minhhai.wms.dao.WarehouseDao;
import com.minhhai.wms.dao.mapper.WarehouseRowMapper;
import com.minhhai.wms.entity.Warehouse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class WarehouseDaoImpl implements WarehouseDao {

    private final JdbcTemplate jdbcTemplate;
    private final WarehouseRowMapper rowMapper = new WarehouseRowMapper();

    public WarehouseDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Warehouse> findAll() {
        return jdbcTemplate.query("SELECT WarehouseID, WarehouseCode, WarehouseName, Address, IsActive FROM Warehouses", rowMapper);
    }

    @Override
    public List<Warehouse> findByIsActive(Boolean isActive) {
        return jdbcTemplate.query(
                "SELECT WarehouseID, WarehouseCode, WarehouseName, Address, IsActive FROM Warehouses WHERE IsActive = ?",
                rowMapper,
                isActive
        );
    }

    @Override
    public Optional<Warehouse> findById(Integer id) {
        List<Warehouse> list = jdbcTemplate.query(
                "SELECT WarehouseID, WarehouseCode, WarehouseName, Address, IsActive FROM Warehouses WHERE WarehouseID = ?",
                rowMapper,
                id
        );
        return list.stream().findFirst();
    }

    @Override
    public boolean existsByWarehouseCode(String warehouseCode) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM Warehouses WHERE WarehouseCode = ?",
                Long.class,
                warehouseCode
        );
        return count != null && count > 0;
    }

    @Override
    public boolean existsByWarehouseCodeAndWarehouseIdNot(String warehouseCode, Integer warehouseId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM Warehouses WHERE WarehouseCode = ? AND WarehouseID <> ?",
                Long.class,
                warehouseCode,
                warehouseId
        );
        return count != null && count > 0;
    }

    @Override
    public List<Warehouse> searchByKeyword(String keyword) {
        String search = "%" + keyword.toLowerCase() + "%";
        return jdbcTemplate.query(
                "SELECT WarehouseID, WarehouseCode, WarehouseName, Address, IsActive FROM Warehouses " +
                        "WHERE LOWER(WarehouseName) LIKE ? OR LOWER(WarehouseCode) LIKE ?",
                rowMapper,
                search,
                search
        );
    }

    @Override
    public Warehouse save(Warehouse warehouse) {
        if (warehouse.getWarehouseId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO Warehouses (WarehouseCode, WarehouseName, Address, IsActive) VALUES (?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, warehouse.getWarehouseCode());
                ps.setString(2, warehouse.getWarehouseName());
                ps.setString(3, warehouse.getAddress());
                ps.setBoolean(4, warehouse.getIsActive() != null ? warehouse.getIsActive() : true);
                return ps;
            }, keyHolder);
            if (keyHolder.getKey() != null) {
                warehouse.setWarehouseId(keyHolder.getKey().intValue());
            }
            return warehouse;
        }
        jdbcTemplate.update(
                "UPDATE Warehouses SET WarehouseCode = ?, WarehouseName = ?, Address = ?, IsActive = ? WHERE WarehouseID = ?",
                warehouse.getWarehouseCode(),
                warehouse.getWarehouseName(),
                warehouse.getAddress(),
                warehouse.getIsActive(),
                warehouse.getWarehouseId()
        );
        return warehouse;
    }

    @Override
    public List<Warehouse> findByIsActiveTrueAndWarehouseIdNot(Integer warehouseId) {
        return jdbcTemplate.query(
                "SELECT WarehouseID, WarehouseCode, WarehouseName, Address, IsActive FROM Warehouses WHERE IsActive = 1 AND WarehouseID <> ?",
                rowMapper,
                warehouseId
        );
    }
}
