package com.minhhai.wms.dao.mapper;

import com.minhhai.wms.entity.TransferNote;
import com.minhhai.wms.entity.User;
import com.minhhai.wms.entity.Warehouse;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class TransferNoteRowMapper implements RowMapper<TransferNote> {

    @Override
    public TransferNote mapRow(ResultSet rs, int rowNum) throws SQLException {
        TransferNote tn = new TransferNote();
        tn.setTnId(rs.getInt("TNID"));
        tn.setTnNumber(rs.getString("TNNumber"));
        tn.setStatus(rs.getString("Status"));

        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        if (createdAt != null) {
            tn.setCreatedAt(createdAt.toLocalDateTime());
        }

        // Warehouse
        Warehouse warehouse = new Warehouse();
        warehouse.setWarehouseId(rs.getInt("WarehouseID"));
        warehouse.setWarehouseCode(rs.getString("WarehouseCode"));
        warehouse.setWarehouseName(rs.getString("WarehouseName"));
        tn.setWarehouse(warehouse);

        // CreatedBy (User)
        Integer createdByUserId = rs.getObject("CreatedBy", Integer.class);
        if (createdByUserId != null) {
            User creator = new User();
            creator.setUserId(createdByUserId);
            creator.setFullName(rs.getString("CreatorFullName"));
            tn.setCreatedBy(creator);
        }

        return tn;
    }
}
