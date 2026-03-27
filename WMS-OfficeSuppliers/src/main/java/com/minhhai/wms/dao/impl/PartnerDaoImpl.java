package com.minhhai.wms.dao.impl;

import com.minhhai.wms.dao.PartnerDao;
import com.minhhai.wms.dao.mapper.PartnerRowMapper;
import com.minhhai.wms.entity.Partner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class PartnerDaoImpl implements PartnerDao {

    private final JdbcTemplate jdbcTemplate;
    private final PartnerRowMapper rowMapper = new PartnerRowMapper();

    public PartnerDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Partner> findAll() {
        return jdbcTemplate.query(
                "SELECT PartnerID, PartnerName, PartnerType, ContactPerson, PhoneNumber, IsActive FROM Partners",
                rowMapper
        );
    }

    @Override
    public List<Partner> findByPartnerType(String partnerType) {
        return jdbcTemplate.query(
                "SELECT PartnerID, PartnerName, PartnerType, ContactPerson, PhoneNumber, IsActive FROM Partners WHERE PartnerType = ?",
                rowMapper,
                partnerType
        );
    }

    @Override
    public List<Partner> findByIsActive(Boolean isActive) {
        return jdbcTemplate.query(
                "SELECT PartnerID, PartnerName, PartnerType, ContactPerson, PhoneNumber, IsActive FROM Partners WHERE IsActive = ?",
                rowMapper,
                isActive
        );
    }

    @Override
    public List<Partner> findByPartnerTypeAndIsActive(String partnerType, Boolean isActive) {
        return jdbcTemplate.query(
                "SELECT PartnerID, PartnerName, PartnerType, ContactPerson, PhoneNumber, IsActive FROM Partners WHERE PartnerType = ? AND IsActive = ?",
                rowMapper,
                partnerType,
                isActive
        );
    }

    @Override
    public List<Partner> searchByName(String keyword) {
        String search = "%" + keyword.toLowerCase() + "%";
        return jdbcTemplate.query(
                "SELECT PartnerID, PartnerName, PartnerType, ContactPerson, PhoneNumber, IsActive FROM Partners " +
                        "WHERE LOWER(PartnerName) LIKE ?",
                rowMapper,
                search
        );
    }

    @Override
    public List<Partner> searchByNameAndType(String keyword, String type) {
        String search = "%" + keyword.toLowerCase() + "%";
        return jdbcTemplate.query(
                "SELECT PartnerID, PartnerName, PartnerType, ContactPerson, PhoneNumber, IsActive FROM Partners " +
                        "WHERE PartnerType = ? AND LOWER(PartnerName) LIKE ?",
                rowMapper,
                type,
                search
        );
    }

    @Override
    public Optional<Partner> findById(Integer id) {
        List<Partner> list = jdbcTemplate.query(
                "SELECT PartnerID, PartnerName, PartnerType, ContactPerson, PhoneNumber, IsActive FROM Partners WHERE PartnerID = ?",
                rowMapper,
                id
        );
        return list.stream().findFirst();
    }

    @Override
    public Partner save(Partner partner) {
        if (partner.getPartnerId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO Partners (PartnerName, PartnerType, ContactPerson, PhoneNumber, IsActive) VALUES (?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, partner.getPartnerName());
                ps.setString(2, partner.getPartnerType());
                ps.setString(3, partner.getContactPerson());
                ps.setString(4, partner.getPhoneNumber());
                ps.setBoolean(5, partner.getIsActive() != null ? partner.getIsActive() : true);
                return ps;
            }, keyHolder);
            if (keyHolder.getKey() != null) {
                partner.setPartnerId(keyHolder.getKey().intValue());
            }
            return partner;
        }
        jdbcTemplate.update(
                "UPDATE Partners SET PartnerName = ?, PartnerType = ?, ContactPerson = ?, PhoneNumber = ?, IsActive = ? WHERE PartnerID = ?",
                partner.getPartnerName(),
                partner.getPartnerType(),
                partner.getContactPerson(),
                partner.getPhoneNumber(),
                partner.getIsActive(),
                partner.getPartnerId()
        );
        return partner;
    }
}
