package com.minhhai.wms.dao.mapper;

import com.minhhai.wms.entity.Warehouse;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WarehouseRowMapper implements RowMapper<Warehouse> {

    @Override
    public Warehouse mapRow(ResultSet rs, int rowNum) throws SQLException {
        Warehouse warehouse = new Warehouse();
        warehouse.setWarehouseId(rs.getInt("WarehouseID"));
        warehouse.setWarehouseCode(rs.getString("WarehouseCode"));
        warehouse.setWarehouseName(rs.getString("WarehouseName"));
        warehouse.setAddress(rs.getString("Address"));
        warehouse.setIsActive(rs.getBoolean("IsActive"));
        return warehouse;
    }
}
