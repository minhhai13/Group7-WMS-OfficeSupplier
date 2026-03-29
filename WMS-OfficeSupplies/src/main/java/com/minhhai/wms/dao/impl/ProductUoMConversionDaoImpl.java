package com.minhhai.wms.dao.impl;

import com.minhhai.wms.dao.ProductUoMConversionDao;
import com.minhhai.wms.dao.mapper.ProductUoMConversionRowMapper;
import com.minhhai.wms.entity.ProductUoMConversion;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class ProductUoMConversionDaoImpl implements ProductUoMConversionDao {

    private final JdbcTemplate jdbcTemplate;
    private final ProductUoMConversionRowMapper rowMapper = new ProductUoMConversionRowMapper();

    public ProductUoMConversionDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ProductUoMConversion> findByProductId(Integer productId) {
        return jdbcTemplate.query(
                "SELECT ConversionID, ProductID, FromUoM, ToUoM, ConversionFactor FROM ProductUoMConversions WHERE ProductID = ?",
                rowMapper,
                productId
        );
    }

    @Override
    public Optional<ProductUoMConversion> findById(Integer id) {
        List<ProductUoMConversion> list = jdbcTemplate.query(
                "SELECT ConversionID, ProductID, FromUoM, ToUoM, ConversionFactor FROM ProductUoMConversions WHERE ConversionID = ?",
                rowMapper,
                id
        );
        return list.stream().findFirst();
    }

    @Override
    public boolean existsByProductIdAndFromUoMAndToUoM(Integer productId, String fromUoM, String toUoM) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ProductUoMConversions WHERE ProductID = ? AND FromUoM = ? AND ToUoM = ?",
                Long.class,
                productId,
                fromUoM,
                toUoM
        );
        return count != null && count > 0;
    }

    @Override
    public boolean existsByProductIdAndFromUoMAndToUoMAndConversionIdNot(
            Integer productId, String fromUoM, String toUoM, Integer conversionId
    ) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM ProductUoMConversions WHERE ProductID = ? AND FromUoM = ? AND ToUoM = ? AND ConversionID <> ?",
                Long.class,
                productId,
                fromUoM,
                toUoM,
                conversionId
        );
        return count != null && count > 0;
    }

    @Override
    public ProductUoMConversion save(ProductUoMConversion conversion) {
        if (conversion.getConversionId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO ProductUoMConversions (ProductID, FromUoM, ToUoM, ConversionFactor) VALUES (?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setInt(1, conversion.getProduct().getProductId());
                ps.setString(2, conversion.getFromUoM());
                ps.setString(3, conversion.getToUoM());
                ps.setInt(4, conversion.getConversionFactor());
                return ps;
            }, keyHolder);
            if (keyHolder.getKey() != null) {
                conversion.setConversionId(keyHolder.getKey().intValue());
            }
            return conversion;
        }
        jdbcTemplate.update(
                "UPDATE ProductUoMConversions SET ProductID = ?, FromUoM = ?, ToUoM = ?, ConversionFactor = ? WHERE ConversionID = ?",
                conversion.getProduct().getProductId(),
                conversion.getFromUoM(),
                conversion.getToUoM(),
                conversion.getConversionFactor(),
                conversion.getConversionId()
        );
        return conversion;
    }

    @Override
    public void deleteById(Integer conversionId) {
        jdbcTemplate.update("DELETE FROM ProductUoMConversions WHERE ConversionID = ?", conversionId);
    }
}
