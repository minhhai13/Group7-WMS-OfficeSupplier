package com.minhhai.wms.dao.mapper;

import com.minhhai.wms.entity.Bin;
import com.minhhai.wms.entity.GoodsIssueDetail;
import com.minhhai.wms.entity.GoodsIssueNote;
import com.minhhai.wms.entity.Product;
import com.minhhai.wms.entity.SalesOrderDetail;
import com.minhhai.wms.entity.TransferOrderDetail;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class GoodsIssueDetailRowMapper implements RowMapper<GoodsIssueDetail> {

    @Override
    public GoodsIssueDetail mapRow(ResultSet rs, int rowNum) throws SQLException {
        GoodsIssueDetail detail = new GoodsIssueDetail();
        detail.setGiDetailId(rs.getInt("GIDetailID"));
        detail.setIssuedQty(rs.getInt("IssuedQty"));
        detail.setPlannedQty(rs.getInt("PlannedQty"));
        detail.setUom(rs.getString("UoM"));
        detail.setBatchNumber(rs.getString("BatchNumber"));

        GoodsIssueNote gin = new GoodsIssueNote();
        gin.setGinId(rs.getInt("GINID"));
        detail.setGoodsIssueNote(gin);

        Product product = new Product();
        product.setProductId(rs.getInt("ProductID"));
        if (hasColumn(rs, "SKU")) {
            product.setSku(rs.getString("SKU"));
            product.setProductName(rs.getString("ProductName"));
            product.setUnitWeight(rs.getBigDecimal("UnitWeight"));
            product.setBaseUoM(rs.getString("BaseUoM"));
        }
        detail.setProduct(product);

        Bin bin = new Bin();
        bin.setBinId(rs.getInt("BinID"));
        if (hasColumn(rs, "BinLocation")) {
            bin.setBinLocation(rs.getString("BinLocation"));
        }
        detail.setBin(bin);

        Integer soDetailId = rs.getObject("SODetailID", Integer.class);
        if (soDetailId != null) {
            SalesOrderDetail soDetail = new SalesOrderDetail();
            soDetail.setSoDetailId(soDetailId);
            if (hasColumn(rs, "SODetailOrderedQty")) {
                soDetail.setOrderedQty(rs.getInt("SODetailOrderedQty"));
                soDetail.setIssuedQty(rs.getInt("SODetailIssuedQty"));
                soDetail.setUom(rs.getString("SODetailUoM"));
            }
            detail.setSalesOrderDetail(soDetail);
        }

        Integer toDetailId = rs.getObject("TODetailID", Integer.class);
        if (toDetailId != null) {
            TransferOrderDetail toDetail = new TransferOrderDetail();
            toDetail.setToDetailId(toDetailId);
            if (hasColumn(rs, "TODetailRequestedQty")) {
                toDetail.setRequestedQty(rs.getInt("TODetailRequestedQty"));
                toDetail.setIssuedQty(rs.getInt("TODetailIssuedQty"));
                toDetail.setReceivedQty(rs.getInt("TODetailReceivedQty"));
                toDetail.setUom(rs.getString("TODetailUoM"));
            }
            detail.setTransferOrderDetail(toDetail);
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
