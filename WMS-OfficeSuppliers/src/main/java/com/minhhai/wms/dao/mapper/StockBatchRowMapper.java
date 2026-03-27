package com.minhhai.wms.dao.mapper;

import com.minhhai.wms.entity.Bin;
import com.minhhai.wms.entity.Product;
import com.minhhai.wms.entity.StockBatch;
import com.minhhai.wms.entity.Warehouse;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class StockBatchRowMapper implements RowMapper<StockBatch> {

    @Override
    public StockBatch mapRow(ResultSet rs, int rowNum) throws SQLException {
        StockBatch batch = new StockBatch();
        batch.setStockBatchId(rs.getInt("StockBatchID"));
        batch.setBatchNumber(rs.getString("BatchNumber"));
        batch.setArrivalDateTime(rs.getTimestamp("ArrivalDateTime").toLocalDateTime());
        batch.setQtyAvailable(rs.getInt("QtyAvailable"));
        batch.setQtyReserved(rs.getInt("QtyReserved"));
        batch.setQtyInTransit(rs.getInt("QtyInTransit"));
        batch.setUom(rs.getString("UoM"));

        Warehouse warehouse = new Warehouse();
        warehouse.setWarehouseId(rs.getInt("WarehouseID"));
        batch.setWarehouse(warehouse);

        Product product = new Product();
        product.setProductId(rs.getInt("ProductID"));
        if (hasColumn(rs, "SKU")) {
            product.setSku(rs.getString("SKU"));
            product.setProductName(rs.getString("ProductName"));
            product.setUnitWeight(rs.getBigDecimal("UnitWeight"));
            product.setBaseUoM(rs.getString("BaseUoM"));
        }
        batch.setProduct(product);

        Bin bin = new Bin();
        bin.setBinId(rs.getInt("BinID"));
        if (hasColumn(rs, "BinLocation")) {
            bin.setBinLocation(rs.getString("BinLocation"));
        }
        batch.setBin(bin);
        return batch;
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
