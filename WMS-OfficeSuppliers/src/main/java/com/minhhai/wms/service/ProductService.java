package com.minhhai.wms.service;

import com.minhhai.wms.dto.ProductDTO;
import com.minhhai.wms.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ProductService {

    List<Product> findAll();

    List<Product> findAllActive();

    Optional<Product> findById(Integer id);

    List<Product> search(String keyword);

    Product save(ProductDTO productDTO);

    Product save(Product product);

    void toggleActive(Integer productId);

}
