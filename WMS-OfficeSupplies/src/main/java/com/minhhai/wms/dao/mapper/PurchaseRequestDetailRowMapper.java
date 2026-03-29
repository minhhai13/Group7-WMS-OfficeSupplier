package com.minhhai.wms.dao.mapper;

import com.minhhai.wms.entity.Product;
import com.minhhai.wms.entity.PurchaseOrderDetail;
import com.minhhai.wms.entity.PurchaseRequest;
import com.minhhai.wms.entity.PurchaseRequestDetail;
import com.minhhai.wms.entity.SalesOrderDetail;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PurchaseRequestDetailRowMapper implements RowMapper<PurchaseRequestDetail> {

    @Override
    public PurchaseRequestDetail mapRow(ResultSet rs, int rowNum) throws SQLException {
        PurchaseRequestDetail detail = new PurchaseRequestDetail();
        detail.setPrDetailId(rs.getInt("PRDetailID"));
        detail.setRequestedQty(rs.getInt("RequestedQty"));
        detail.setUom(rs.getString("UoM"));

        PurchaseRequest pr = new PurchaseRequest();
        pr.setPrId(rs.getInt("PRID"));
        detail.setPurchaseRequest(pr);

        Product product = new Product();
        product.setProductId(rs.getInt("ProductID"));
        if (hasColumn(rs, "SKU")) {
            product.setSku(rs.getString("SKU"));
            product.setProductName(rs.getString("ProductName"));
            product.setBaseUoM(rs.getString("BaseUoM"));
            product.setUnitWeight(rs.getBigDecimal("UnitWeight"));
        }
        detail.setProduct(product);

        Integer soDetailId = rs.getObject("SODetailID", Integer.class);
        if (soDetailId != null) {
            SalesOrderDetail soDetail = new SalesOrderDetail();
            soDetail.setSoDetailId(soDetailId);
            detail.setSalesOrderDetail(soDetail);
        }

        Integer poDetailId = rs.getObject("PODetailID", Integer.class);
        if (poDetailId != null) {
            PurchaseOrderDetail poDetail = new PurchaseOrderDetail();
            poDetail.setPoDetailId(poDetailId);
            detail.setPurchaseOrderDetail(poDetail);
        }
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
