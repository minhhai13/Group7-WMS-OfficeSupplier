package com.minhhai.wms.dao.mapper;

import com.minhhai.wms.entity.Product;
import com.minhhai.wms.entity.ProductUoMConversion;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ProductUoMConversionRowMapper implements RowMapper<ProductUoMConversion> {

    @Override
    public ProductUoMConversion mapRow(ResultSet rs, int rowNum) throws SQLException {
        ProductUoMConversion conversion = new ProductUoMConversion();
        conversion.setConversionId(rs.getInt("ConversionID"));
        conversion.setFromUoM(rs.getString("FromUoM"));
        conversion.setToUoM(rs.getString("ToUoM"));
        conversion.setConversionFactor(rs.getInt("ConversionFactor"));
        Integer productId = rs.getObject("ProductID", Integer.class);
        if (productId != null) {
            Product product = new Product();
            product.setProductId(productId);
            conversion.setProduct(product);
        }
        return conversion;
    }
}
