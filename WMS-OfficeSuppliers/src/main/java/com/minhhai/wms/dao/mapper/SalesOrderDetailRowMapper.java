package com.minhhai.wms.dao.mapper;

import com.minhhai.wms.entity.Product;
import com.minhhai.wms.entity.SalesOrder;
import com.minhhai.wms.entity.SalesOrderDetail;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SalesOrderDetailRowMapper implements RowMapper<SalesOrderDetail> {

    @Override
    public SalesOrderDetail mapRow(ResultSet rs, int rowNum) throws SQLException {
        SalesOrderDetail detail = new SalesOrderDetail();
        detail.setSoDetailId(rs.getInt("SODetailID"));
        detail.setOrderedQty(rs.getInt("OrderedQty"));
        detail.setIssuedQty(rs.getInt("IssuedQty"));
        detail.setUom(rs.getString("UoM"));

        SalesOrder so = new SalesOrder();
        so.setSoId(rs.getInt("SOID"));
        detail.setSalesOrder(so);

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
