package com.minhhai.wms.service.impl;

import com.minhhai.wms.dao.ProductDao;
import com.minhhai.wms.entity.Product;
import com.minhhai.wms.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductDao productDao;

    @Override
    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productDao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findAllActive() {
        return productDao.findByIsActive(true);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findById(Integer id) {
        return productDao.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }
        return productDao.searchByKeyword(keyword.trim());
    }

    @Override
    public Product save(Product product) {
        return productDao.save(product);
    }

    @Override
    public Product save(com.minhhai.wms.dto.ProductDTO productDTO) {
        // Uniqueness check
        if (productDTO.getProductId() == null) {
            if (productDao.existsBySku(productDTO.getSku())) {
                throw new IllegalArgumentException("SKU already exists.");
            }
        } else {
            if (productDao.existsBySkuAndProductIdNot(productDTO.getSku(), productDTO.getProductId())) {
                throw new IllegalArgumentException("SKU already exists.");
            }
        }

        Product product;
        if (productDTO.getProductId() != null) {
            product = productDao.findById(productDTO.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productDTO.getProductId()));
            product.setSku(productDTO.getSku());
            product.setProductName(productDTO.getProductName());
            product.setUnitWeight(productDTO.getUnitWeight());
            product.setBaseUoM(productDTO.getBaseUoM());
            product.setMinStockLevel(productDTO.getMinStockLevel());
        } else {
            product = Product.builder()
                    .sku(productDTO.getSku())
                    .productName(productDTO.getProductName())
                    .unitWeight(productDTO.getUnitWeight())
                    .baseUoM(productDTO.getBaseUoM())
                    .minStockLevel(productDTO.getMinStockLevel())
                    .isActive(true)
                    .build();
        }

        return productDao.save(product);
    }

    @Override
    public void toggleActive(Integer productId) {
        Product product = productDao.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        product.setIsActive(!product.getIsActive());
        productDao.save(product);
    }
}
