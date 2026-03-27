package com.minhhai.wms.dao;

import com.minhhai.wms.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ProductDao {

    List<Product> findAll();

    List<Product> findByIsActive(Boolean isActive);

    Optional<Product> findById(Integer id);

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    boolean existsBySkuAndProductIdNot(String sku, Integer productId);

    List<Product> searchByKeyword(String keyword);

    Product save(Product product);
}
