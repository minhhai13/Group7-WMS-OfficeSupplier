package com.minhhai.wms.dao.mapper;

import com.minhhai.wms.entity.Partner;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PartnerRowMapper implements RowMapper<Partner> {

    @Override
    public Partner mapRow(ResultSet rs, int rowNum) throws SQLException {
        Partner partner = new Partner();
        partner.setPartnerId(rs.getInt("PartnerID"));
        partner.setPartnerName(rs.getString("PartnerName"));
        partner.setPartnerType(rs.getString("PartnerType"));
        partner.setContactPerson(rs.getString("ContactPerson"));
        partner.setPhoneNumber(rs.getString("PhoneNumber"));
        partner.setIsActive(rs.getBoolean("IsActive"));
        return partner;
    }
}
