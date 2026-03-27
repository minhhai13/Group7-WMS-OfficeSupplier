package com.minhhai.wms.dao.impl;

import com.minhhai.wms.dao.ProductDao;
import com.minhhai.wms.dao.mapper.ProductRowMapper;
import com.minhhai.wms.entity.Product;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class ProductDaoImpl implements ProductDao {

    private final JdbcTemplate jdbcTemplate;
    private final ProductRowMapper rowMapper = new ProductRowMapper();

    public ProductDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Product> findAll() {
        return jdbcTemplate.query(
                "SELECT ProductID, SKU, ProductName, UnitWeight, BaseUoM, MinStockLevel, IsActive FROM Products",
                rowMapper
        );
    }

    @Override
    public List<Product> findByIsActive(Boolean isActive) {
        return jdbcTemplate.query(
                "SELECT ProductID, SKU, ProductName, UnitWeight, BaseUoM, MinStockLevel, IsActive FROM Products WHERE IsActive = ?",
                rowMapper,
                isActive
        );
    }

    @Override
    public Optional<Product> findById(Integer id) {
        List<Product> list = jdbcTemplate.query(
                "SELECT ProductID, SKU, ProductName, UnitWeight, BaseUoM, MinStockLevel, IsActive FROM Products WHERE ProductID = ?",
                rowMapper,
                id
        );
        return list.stream().findFirst();
    }

    @Override
    public Optional<Product> findBySku(String sku) {
        List<Product> list = jdbcTemplate.query(
                "SELECT ProductID, SKU, ProductName, UnitWeight, BaseUoM, MinStockLevel, IsActive FROM Products WHERE SKU = ?",
                rowMapper,
                sku
        );
        return list.stream().findFirst();
    }

    @Override
    public boolean existsBySku(String sku) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM Products WHERE SKU = ?", Long.class, sku);
        return count != null && count > 0;
    }

    @Override
    public boolean existsBySkuAndProductIdNot(String sku, Integer productId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM Products WHERE SKU = ? AND ProductID <> ?",
                Long.class,
                sku,
                productId
        );
        return count != null && count > 0;
    }

    @Override
    public List<Product> searchByKeyword(String keyword) {
        String search = "%" + keyword.toLowerCase() + "%";
        return jdbcTemplate.query(
                "SELECT ProductID, SKU, ProductName, UnitWeight, BaseUoM, MinStockLevel, IsActive FROM Products " +
                        "WHERE LOWER(SKU) LIKE ? OR LOWER(ProductName) LIKE ?",
                rowMapper,
                search,
                search
        );
    }

    @Override
    public Product save(Product product) {
        if (product.getProductId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO Products (SKU, ProductName, BaseUoM, UnitWeight, MinStockLevel, IsActive) VALUES (?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, product.getSku());
                ps.setString(2, product.getProductName());
                ps.setString(3, product.getBaseUoM());
                ps.setBigDecimal(4, product.getUnitWeight());
                ps.setInt(5, product.getMinStockLevel() != null ? product.getMinStockLevel() : 0);
                ps.setBoolean(6, product.getIsActive() != null ? product.getIsActive() : true);
                return ps;
            }, keyHolder);
            if (keyHolder.getKey() != null) {
                product.setProductId(keyHolder.getKey().intValue());
            }
            return product;
        }
        jdbcTemplate.update(
                "UPDATE Products SET SKU = ?, ProductName = ?, BaseUoM = ?, UnitWeight = ?, MinStockLevel = ?, IsActive = ? WHERE ProductID = ?",
                product.getSku(),
                product.getProductName(),
                product.getBaseUoM(),
                product.getUnitWeight(),
                product.getMinStockLevel(),
                product.getIsActive(),
                product.getProductId()
        );
        return product;
    }
}
