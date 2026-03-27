package com.minhhai.wms.dao.mapper;

import com.minhhai.wms.entity.TransferOrder;
import com.minhhai.wms.entity.User;
import com.minhhai.wms.entity.Warehouse;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TransferOrderRowMapper implements RowMapper<TransferOrder> {

    @Override
    public TransferOrder mapRow(ResultSet rs, int rowNum) throws SQLException {
        TransferOrder to = new TransferOrder();
        to.setToId(rs.getInt("TOID"));
        to.setToNumber(rs.getString("TONumber"));
        to.setStatus(rs.getString("Status"));
        to.setRejectReason(rs.getString("RejectReason"));

        Warehouse source = new Warehouse();
        source.setWarehouseId(rs.getInt("SourceWarehouseID"));
        source.setWarehouseName(rs.getString("SourceWarehouseName"));
        to.setSourceWarehouse(source);

        Warehouse dest = new Warehouse();
        dest.setWarehouseId(rs.getInt("DestinationWarehouseID"));
        dest.setWarehouseName(rs.getString("DestinationWarehouseName"));
        to.setDestinationWarehouse(dest);

        Integer createdBy = rs.getObject("CreatedBy", Integer.class);
        if (createdBy != null) {
            User creator = new User();
            creator.setUserId(createdBy);
            creator.setFullName(rs.getString("CreatorFullName"));
            to.setCreatedBy(creator);
        }

        return to;
    }
}
