package com.minhhai.wms.dao.mapper;

import com.minhhai.wms.entity.Product;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ProductRowMapper implements RowMapper<Product> {

    @Override
    public Product mapRow(ResultSet rs, int rowNum) throws SQLException {
        Product product = new Product();
        product.setProductId(rs.getInt("ProductID"));
        product.setSku(rs.getString("SKU"));
        product.setProductName(rs.getString("ProductName"));
        product.setUnitWeight(rs.getBigDecimal("UnitWeight"));
        product.setBaseUoM(rs.getString("BaseUoM"));
        product.setMinStockLevel(rs.getInt("MinStockLevel"));
        product.setIsActive(rs.getBoolean("IsActive"));
        return product;
    }
}
