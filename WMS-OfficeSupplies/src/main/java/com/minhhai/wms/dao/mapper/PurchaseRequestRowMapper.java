package com.minhhai.wms.dao.mapper;

import com.minhhai.wms.entity.PurchaseOrder;
import com.minhhai.wms.entity.PurchaseRequest;
import com.minhhai.wms.entity.SalesOrder;
import com.minhhai.wms.entity.Warehouse;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PurchaseRequestRowMapper implements RowMapper<PurchaseRequest> {

    @Override
    public PurchaseRequest mapRow(ResultSet rs, int rowNum) throws SQLException {
        PurchaseRequest pr = new PurchaseRequest();
        pr.setPrId(rs.getInt("PRID"));
        pr.setPrNumber(rs.getString("PRNumber"));
        pr.setStatus(rs.getString("Status"));

        Warehouse warehouse = new Warehouse();
        warehouse.setWarehouseId(rs.getInt("WarehouseID"));
        pr.setWarehouse(warehouse);

        Integer soId = rs.getObject("RelatedSOID", Integer.class);
        if (soId != null) {
            SalesOrder so = new SalesOrder();
            so.setSoId(soId);
            pr.setRelatedSalesOrder(so);
        }

        Integer poId = rs.getObject("POID", Integer.class);
        if (poId != null) {
            PurchaseOrder po = new PurchaseOrder();
            po.setPoId(poId);
            pr.setPurchaseOrder(po);
        }
        return pr;
    }
}
