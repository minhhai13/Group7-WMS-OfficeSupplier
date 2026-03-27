package com.minhhai.wms.dao.mapper;

import com.minhhai.wms.entity.Partner;
import com.minhhai.wms.entity.SalesOrder;
import com.minhhai.wms.entity.Warehouse;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SalesOrderRowMapper implements RowMapper<SalesOrder> {

    @Override
    public SalesOrder mapRow(ResultSet rs, int rowNum) throws SQLException {
        SalesOrder so = new SalesOrder();
        so.setSoId(rs.getInt("SOID"));
        so.setSoNumber(rs.getString("SONumber"));
        so.setSoStatus(rs.getString("SOStatus"));
        so.setRejectReason(rs.getString("RejectReason"));

        Warehouse warehouse = new Warehouse();
        warehouse.setWarehouseId(rs.getInt("WarehouseID"));
        if (hasColumn(rs, "WarehouseName")) {
            warehouse.setWarehouseName(rs.getString("WarehouseName"));
            warehouse.setWarehouseCode(rs.getString("WarehouseCode"));
            warehouse.setAddress(rs.getString("Address"));
            warehouse.setIsActive(rs.getBoolean("WarehouseIsActive"));
        }
        so.setWarehouse(warehouse);

        Partner customer = new Partner();
        customer.setPartnerId(rs.getInt("CustomerID"));
        if (hasColumn(rs, "PartnerName")) {
            customer.setPartnerName(rs.getString("PartnerName"));
            customer.setPartnerType(rs.getString("PartnerType"));
            customer.setContactPerson(rs.getString("ContactPerson"));
            customer.setPhoneNumber(rs.getString("PhoneNumber"));
            customer.setIsActive(rs.getBoolean("CustomerIsActive"));
        }
        so.setCustomer(customer);
        return so;
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
