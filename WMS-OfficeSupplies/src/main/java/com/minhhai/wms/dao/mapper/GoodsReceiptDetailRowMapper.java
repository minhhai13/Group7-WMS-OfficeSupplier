package com.minhhai.wms.dao.mapper;

import com.minhhai.wms.entity.Bin;
import com.minhhai.wms.entity.GoodsReceiptDetail;
import com.minhhai.wms.entity.GoodsReceiptNote;
import com.minhhai.wms.entity.Product;
import com.minhhai.wms.entity.PurchaseOrderDetail;
import com.minhhai.wms.entity.TransferOrderDetail;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class GoodsReceiptDetailRowMapper implements RowMapper<GoodsReceiptDetail> {

    @Override
    public GoodsReceiptDetail mapRow(ResultSet rs, int rowNum) throws SQLException {
        GoodsReceiptDetail detail = new GoodsReceiptDetail();
        detail.setGrDetailId(rs.getInt("GRDetailID"));
        detail.setReceivedQty(rs.getInt("ReceivedQty"));
        detail.setExpectedQty(rs.getInt("ExpectedQty"));
        detail.setUom(rs.getString("UoM"));
        detail.setBatchNumber(rs.getString("BatchNumber"));

        GoodsReceiptNote grn = new GoodsReceiptNote();
        grn.setGrnId(rs.getInt("GRNID"));
        detail.setGoodsReceiptNote(grn);

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

        Integer poDetailId = rs.getObject("PODetailID", Integer.class);
        if (poDetailId != null) {
            PurchaseOrderDetail poDetail = new PurchaseOrderDetail();
            poDetail.setPoDetailId(poDetailId);
            if (hasColumn(rs, "PODetailOrderedQty")) {
                poDetail.setOrderedQty(rs.getInt("PODetailOrderedQty"));
                poDetail.setReceivedQty(rs.getInt("PODetailReceivedQty"));
                poDetail.setUom(rs.getString("PODetailUoM"));
            }
            detail.setPurchaseOrderDetail(poDetail);
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
