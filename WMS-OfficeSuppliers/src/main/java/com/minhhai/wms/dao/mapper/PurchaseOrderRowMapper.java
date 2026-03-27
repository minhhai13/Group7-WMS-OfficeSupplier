package com.minhhai.wms.dao.mapper;

import com.minhhai.wms.entity.Partner;
import com.minhhai.wms.entity.PurchaseOrder;
import com.minhhai.wms.entity.Warehouse;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PurchaseOrderRowMapper implements RowMapper<PurchaseOrder> {

    @Override
    public PurchaseOrder mapRow(ResultSet rs, int rowNum) throws SQLException {
        PurchaseOrder po = new PurchaseOrder();
        po.setPoId(rs.getInt("POID"));
        po.setPoNumber(rs.getString("PONumber"));
        po.setPoStatus(rs.getString("POStatus"));
        po.setRejectReason(rs.getString("RejectReason"));

        Warehouse warehouse = new Warehouse();
        warehouse.setWarehouseId(rs.getInt("WarehouseID"));
        if (hasColumn(rs, "WarehouseName")) {
            warehouse.setWarehouseName(rs.getString("WarehouseName"));
            warehouse.setWarehouseCode(rs.getString("WarehouseCode"));
            warehouse.setAddress(rs.getString("Address"));
            warehouse.setIsActive(rs.getBoolean("WarehouseIsActive"));
        }
        po.setWarehouse(warehouse);

        Partner supplier = new Partner();
        supplier.setPartnerId(rs.getInt("SupplierID"));
        if (hasColumn(rs, "PartnerName")) {
            supplier.setPartnerName(rs.getString("PartnerName"));
            supplier.setPartnerType(rs.getString("PartnerType"));
            supplier.setContactPerson(rs.getString("ContactPerson"));
            supplier.setPhoneNumber(rs.getString("PhoneNumber"));
            supplier.setIsActive(rs.getBoolean("SupplierIsActive"));
        }
        po.setSupplier(supplier);
        return po;
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
