package com.minhhai.wms.dao.mapper;

import com.minhhai.wms.entity.GoodsIssueNote;
import com.minhhai.wms.entity.Partner;
import com.minhhai.wms.entity.SalesOrder;
import com.minhhai.wms.entity.TransferOrder;
import com.minhhai.wms.entity.Warehouse;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class GoodsIssueNoteRowMapper implements RowMapper<GoodsIssueNote> {

    @Override
    public GoodsIssueNote mapRow(ResultSet rs, int rowNum) throws SQLException {
        GoodsIssueNote gin = new GoodsIssueNote();
        gin.setGinId(rs.getInt("GINID"));
        gin.setGinNumber(rs.getString("GINNumber"));
        gin.setGiStatus(rs.getString("GIStatus"));

        Warehouse warehouse = new Warehouse();
        warehouse.setWarehouseId(rs.getInt("WarehouseID"));
        if (hasColumn(rs, "WarehouseName")) {
            warehouse.setWarehouseName(rs.getString("WarehouseName"));
        }
        gin.setWarehouse(warehouse);

        Integer soId = rs.getObject("SOID", Integer.class);
        if (soId != null) {
            SalesOrder so = new SalesOrder();
            so.setSoId(soId);
            if (hasColumn(rs, "SONumber")) {
                so.setSoNumber(rs.getString("SONumber"));
            }
            if (hasColumn(rs, "CustomerID")) {
                Partner customer = new Partner();
                customer.setPartnerId(rs.getInt("CustomerID"));
                if (hasColumn(rs, "CustomerName")) {
                    customer.setPartnerName(rs.getString("CustomerName"));
                }
                so.setCustomer(customer);
            }
            gin.setSalesOrder(so);
        }

        Integer toId = rs.getObject("TransferOrderID", Integer.class);
        if (toId != null) {
            TransferOrder to = new TransferOrder();
            to.setToId(toId);
            if (hasColumn(rs, "TONumber")) {
                to.setToNumber(rs.getString("TONumber"));
            }
            if (hasColumn(rs, "DestinationWarehouseID")) {
                Warehouse dest = new Warehouse();
                dest.setWarehouseId(rs.getInt("DestinationWarehouseID"));
                if (hasColumn(rs, "DestinationWarehouseName")) {
                    dest.setWarehouseName(rs.getString("DestinationWarehouseName"));
                }
                to.setDestinationWarehouse(dest);
            }
            gin.setTransferOrder(to);
        }
        return gin;
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
