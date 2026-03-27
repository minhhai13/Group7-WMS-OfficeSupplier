package com.minhhai.wms.dao.mapper;

import com.minhhai.wms.entity.Bin;
import com.minhhai.wms.entity.Warehouse;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class BinRowMapper implements RowMapper<Bin> {

    @Override
    public Bin mapRow(ResultSet rs, int rowNum) throws SQLException {
        Bin bin = new Bin();
        bin.setBinId(rs.getInt("BinID"));
        bin.setBinLocation(rs.getString("BinLocation"));
        bin.setMaxWeight(rs.getBigDecimal("MaxWeight"));
        bin.setIsActive(rs.getBoolean("IsActive"));

        Warehouse warehouse = new Warehouse();
        warehouse.setWarehouseId(rs.getInt("WarehouseID"));
        if (hasColumn(rs, "WarehouseCode")) {
            warehouse.setWarehouseCode(rs.getString("WarehouseCode"));
            warehouse.setWarehouseName(rs.getString("WarehouseName"));
            warehouse.setAddress(rs.getString("Address"));
            warehouse.setIsActive(rs.getBoolean("WarehouseIsActive"));
        }
        bin.setWarehouse(warehouse);
        return bin;
    }

    private boolean hasColumn(ResultSet rs, String column) throws SQLException {
        try {
            rs.findColumn(column);
            return true;
        } catch (SQLException ex) {
            return false;
        }
    }
}
