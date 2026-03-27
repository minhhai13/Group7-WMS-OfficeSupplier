package com.minhhai.wms.dao.mapper;

import com.minhhai.wms.entity.User;
import com.minhhai.wms.entity.Warehouse;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRowMapper implements RowMapper<User> {

    @Override
    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("UserID"));
        user.setUsername(rs.getString("Username"));
        user.setPasswordHash(rs.getString("PasswordHash"));
        user.setFullName(rs.getString("FullName"));
        user.setRole(rs.getString("Role"));
        user.setIsActive(rs.getBoolean("IsActive"));

        Integer warehouseId = rs.getObject("WarehouseID", Integer.class);
        if (warehouseId != null) {
            Warehouse warehouse = new Warehouse();
            warehouse.setWarehouseId(warehouseId);
            if (hasColumn(rs, "WarehouseCode")) {
                warehouse.setWarehouseCode(rs.getString("WarehouseCode"));
                warehouse.setWarehouseName(rs.getString("WarehouseName"));
                warehouse.setAddress(rs.getString("Address"));
                warehouse.setIsActive(rs.getBoolean("WarehouseIsActive"));
            }
            user.setWarehouse(warehouse);
        }
        return user;
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
