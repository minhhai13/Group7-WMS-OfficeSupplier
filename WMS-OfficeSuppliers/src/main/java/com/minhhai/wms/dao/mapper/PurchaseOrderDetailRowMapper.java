package com.minhhai.wms.dao.mapper;

import com.minhhai.wms.entity.Product;
import com.minhhai.wms.entity.PurchaseOrder;
import com.minhhai.wms.entity.PurchaseOrderDetail;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PurchaseOrderDetailRowMapper implements RowMapper<PurchaseOrderDetail> {

    @Override
    public PurchaseOrderDetail mapRow(ResultSet rs, int rowNum) throws SQLException {
        PurchaseOrderDetail detail = new PurchaseOrderDetail();
        detail.setPoDetailId(rs.getInt("PODetailID"));
        detail.setOrderedQty(rs.getInt("OrderedQty"));
        detail.setReceivedQty(rs.getInt("ReceivedQty"));
        detail.setUom(rs.getString("UoM"));

        PurchaseOrder po = new PurchaseOrder();
        po.setPoId(rs.getInt("POID"));
        detail.setPurchaseOrder(po);

        Product product = new Product();
        product.setProductId(rs.getInt("ProductID"));
        if (hasColumn(rs, "SKU")) {
            product.setSku(rs.getString("SKU"));
            product.setProductName(rs.getString("ProductName"));
            product.setUnitWeight(rs.getBigDecimal("UnitWeight"));
            product.setBaseUoM(rs.getString("BaseUoM"));
            product.setMinStockLevel(rs.getInt("MinStockLevel"));
            product.setIsActive(rs.getBoolean("ProductIsActive"));
        }
        detail.setProduct(product);
        return detail;
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
