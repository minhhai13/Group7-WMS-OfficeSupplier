package com.minhhai.wms.dao.mapper;

import com.minhhai.wms.entity.GoodsReceiptNote;
import com.minhhai.wms.entity.Partner;
import com.minhhai.wms.entity.PurchaseOrder;
import com.minhhai.wms.entity.TransferOrder;
import com.minhhai.wms.entity.Warehouse;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class GoodsReceiptNoteRowMapper implements RowMapper<GoodsReceiptNote> {

    @Override
    public GoodsReceiptNote mapRow(ResultSet rs, int rowNum) throws SQLException {
        GoodsReceiptNote grn = new GoodsReceiptNote();
        grn.setGrnId(rs.getInt("GRNID"));
        grn.setGrnNumber(rs.getString("GRNNumber"));
        grn.setGrStatus(rs.getString("GRStatus"));

        Warehouse warehouse = new Warehouse();
        warehouse.setWarehouseId(rs.getInt("WarehouseID"));
        if (hasColumn(rs, "WarehouseName")) {
            warehouse.setWarehouseName(rs.getString("WarehouseName"));
        }
        grn.setWarehouse(warehouse);

        Integer poId = rs.getObject("POID", Integer.class);
        if (poId != null) {
            PurchaseOrder po = new PurchaseOrder();
            po.setPoId(poId);
            if (hasColumn(rs, "PONumber")) {
                po.setPoNumber(rs.getString("PONumber"));
            }
            if (hasColumn(rs, "SupplierID")) {
                Partner supplier = new Partner();
                supplier.setPartnerId(rs.getInt("SupplierID"));
                if (hasColumn(rs, "SupplierName")) {
                    supplier.setPartnerName(rs.getString("SupplierName"));
                }
                po.setSupplier(supplier);
            }
            grn.setPurchaseOrder(po);
        }

        Integer toId = rs.getObject("TransferOrderID", Integer.class);
        if (toId != null) {
            TransferOrder to = new TransferOrder();
            to.setToId(toId);
            if (hasColumn(rs, "TONumber")) {
                to.setToNumber(rs.getString("TONumber"));
            }
            if (hasColumn(rs, "SourceWarehouseID")) {
                Warehouse source = new Warehouse();
                source.setWarehouseId(rs.getInt("SourceWarehouseID"));
                if (hasColumn(rs, "SourceWarehouseName")) {
                    source.setWarehouseName(rs.getString("SourceWarehouseName"));
                }
                to.setSourceWarehouse(source);
            }
            if (hasColumn(rs, "DestinationWarehouseID")) {
                Warehouse dest = new Warehouse();
                dest.setWarehouseId(rs.getInt("DestinationWarehouseID"));
                if (hasColumn(rs, "DestinationWarehouseName")) {
                    dest.setWarehouseName(rs.getString("DestinationWarehouseName"));
                }
                to.setDestinationWarehouse(dest);
            }
            if (hasColumn(rs, "TOStatus")) {
                to.setStatus(rs.getString("TOStatus"));
            }
            grn.setTransferOrder(to);
        }
        return grn;
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
