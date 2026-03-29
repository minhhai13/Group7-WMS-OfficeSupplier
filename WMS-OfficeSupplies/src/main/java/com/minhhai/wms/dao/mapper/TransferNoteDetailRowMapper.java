package com.minhhai.wms.dao.mapper;

import com.minhhai.wms.entity.Bin;
import com.minhhai.wms.entity.Product;
import com.minhhai.wms.entity.TransferNoteDetail;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TransferNoteDetailRowMapper implements RowMapper<TransferNoteDetail> {

    @Override
    public TransferNoteDetail mapRow(ResultSet rs, int rowNum) throws SQLException {
        TransferNoteDetail detail = new TransferNoteDetail();
        detail.setTnDetailId(rs.getInt("TNDetailID"));
        detail.setBatchNumber(rs.getString("BatchNumber"));
        detail.setQuantity(rs.getInt("Quantity"));
        detail.setUom(rs.getString("UoM"));

        // Product
        Product product = new Product();
        product.setProductId(rs.getInt("ProductID"));
        product.setSku(rs.getString("SKU"));
        product.setProductName(rs.getString("ProductName"));
        product.setUnitWeight(rs.getBigDecimal("UnitWeight"));
        product.setBaseUoM(rs.getString("BaseUoM"));
        detail.setProduct(product);

        // FromBin
        Bin fromBin = new Bin();
        fromBin.setBinId(rs.getInt("FromBinID"));
        fromBin.setBinLocation(rs.getString("FromBinLocation"));
        detail.setFromBin(fromBin);

        // ToBin
        Bin toBin = new Bin();
        toBin.setBinId(rs.getInt("ToBinID"));
        toBin.setBinLocation(rs.getString("ToBinLocation"));
        detail.setToBin(toBin);

        return detail;
    }
}
