package com.minhhai.wms.dao.impl;

import com.minhhai.wms.dao.BinDao;
import com.minhhai.wms.dao.mapper.BinRowMapper;
import com.minhhai.wms.entity.Bin;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class BinDaoImpl implements BinDao {

    private static final String BASE_SELECT = """
            SELECT b.BinID, b.WarehouseID, b.BinLocation, b.MaxWeight, b.IsActive,
                   w.WarehouseCode, w.WarehouseName, w.Address, w.IsActive AS WarehouseIsActive
            FROM Bins b
            JOIN Warehouses w ON b.WarehouseID = w.WarehouseID
            """;

    private final JdbcTemplate jdbcTemplate;
    private final BinRowMapper rowMapper = new BinRowMapper();

    public BinDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Bin> findByWarehouseId(Integer warehouseId) {
        return jdbcTemplate.query(BASE_SELECT + " WHERE b.WarehouseID = ?", rowMapper, warehouseId);
    }

    @Override
    public List<Bin> findByWarehouseIdAndIsActive(Integer warehouseId, Boolean isActive) {
        return jdbcTemplate.query(
                BASE_SELECT + " WHERE b.WarehouseID = ? AND b.IsActive = ?",
                rowMapper,
                warehouseId,
                isActive
        );
    }

    @Override
    public List<Bin> findByWarehouseIdAndIsActiveForUpdate(Integer warehouseId, Boolean isActive) {
        // [RACE CONDITION FIX]: Vượt ngưỡng MaxWeight (Overflow Capacity)
        // Dùng FOR UPDATE để khóa toàn bộ Bins active của kho lại trong 1 transaction.
        // Khi nhiều Purchase Order cùng duyệt và chạy hàm cấp phát Bin tự động,
        // các transaction sẽ phải xếp hàng, tránh tình trạng nhiều PO lấy cùng 1 mức sức chứa trống và nhét lố hàng vào Bin.
        String sql = """
                SELECT b.BinID, b.WarehouseID, b.BinLocation, b.MaxWeight, b.IsActive,
                       w.WarehouseCode, w.WarehouseName, w.Address, w.IsActive AS WarehouseIsActive
                FROM Bins b
                JOIN Warehouses w ON b.WarehouseID = w.WarehouseID
                WHERE b.WarehouseID = ? AND b.IsActive = ?
                FOR UPDATE
                """;
        return jdbcTemplate.query(sql, rowMapper, warehouseId, isActive);
    }

    @Override
    public Optional<Bin> findById(Integer id) {
        List<Bin> list = jdbcTemplate.query(BASE_SELECT + " WHERE b.BinID = ?", rowMapper, id);
        return list.stream().findFirst();
    }

    @Override
    public List<Bin> searchInWarehouse(Integer warehouseId, String keyword) {
        String search = "%" + keyword.toLowerCase() + "%";
        return jdbcTemplate.query(
                BASE_SELECT + " WHERE b.WarehouseID = ? AND LOWER(b.BinLocation) LIKE ?",
                rowMapper,
                warehouseId,
                search
        );
    }

    @Override
    public boolean existsByWarehouseIdAndBinLocation(Integer warehouseId, String binLocation) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM Bins WHERE WarehouseID = ? AND BinLocation = ?",
                Long.class,
                warehouseId,
                binLocation
        );
        return count != null && count > 0;
    }

    @Override
    public boolean existsByWarehouseIdAndBinLocationAndBinIdNot(Integer warehouseId, String binLocation, Integer binId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM Bins WHERE WarehouseID = ? AND BinLocation = ? AND BinID <> ?",
                Long.class,
                warehouseId,
                binLocation,
                binId
        );
        return count != null && count > 0;
    }

    @Override
    public Bin save(Bin bin) {
        if (bin.getBinId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO Bins (WarehouseID, BinLocation, MaxWeight, IsActive) VALUES (?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setInt(1, bin.getWarehouse().getWarehouseId());
                ps.setString(2, bin.getBinLocation());
                ps.setBigDecimal(3, bin.getMaxWeight());
                ps.setBoolean(4, bin.getIsActive() != null ? bin.getIsActive() : true);
                return ps;
            }, keyHolder);
            if (keyHolder.getKey() != null) {
                bin.setBinId(keyHolder.getKey().intValue());
            }
            return bin;
        }
        jdbcTemplate.update(
                "UPDATE Bins SET WarehouseID = ?, BinLocation = ?, MaxWeight = ?, IsActive = ? WHERE BinID = ?",
                bin.getWarehouse().getWarehouseId(),
                bin.getBinLocation(),
                bin.getMaxWeight(),
                bin.getIsActive(),
                bin.getBinId()
        );
        return bin;
    }
}
