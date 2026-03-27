package com.minhhai.wms.dao.mapper;

import com.minhhai.wms.entity.Product;
import com.minhhai.wms.entity.TransferOrder;
import com.minhhai.wms.entity.TransferOrderDetail;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TransferOrderDetailRowMapper implements RowMapper<TransferOrderDetail> {

    @Override
    public TransferOrderDetail mapRow(ResultSet rs, int rowNum) throws SQLException {
        TransferOrderDetail detail = new TransferOrderDetail();
        detail.setToDetailId(rs.getInt("TODetailID"));
        detail.setRequestedQty(rs.getInt("RequestedQty"));
        detail.setIssuedQty(rs.getInt("IssuedQty"));
        detail.setReceivedQty(rs.getInt("ReceivedQty"));
        detail.setUom(rs.getString("UoM"));

        TransferOrder to = new TransferOrder();
        to.setToId(rs.getInt("TOID"));
        detail.setTransferOrder(to);

        Product product = new Product();
        product.setProductId(rs.getInt("ProductID"));
        if (hasColumn(rs, "SKU")) {
            product.setSku(rs.getString("SKU"));
            product.setProductName(rs.getString("ProductName"));
            product.setUnitWeight(rs.getBigDecimal("UnitWeight"));
            product.setBaseUoM(rs.getString("BaseUoM"));
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
