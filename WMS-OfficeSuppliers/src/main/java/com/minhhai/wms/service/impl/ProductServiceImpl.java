package com.minhhai.wms.service.impl;

import com.minhhai.wms.entity.Product;
import com.minhhai.wms.repository.ProductRepository;
import com.minhhai.wms.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> findAllActive() {
        return productRepository.findByIsActive(true);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findById(Integer id) {
        return productRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }
        return productRepository.searchByKeyword(keyword.trim());
    }

    @Override
    public Product save(Product product) {
        return productRepository.save(product);
    }

    @Override
    public Product save(com.minhhai.wms.dto.ProductDTO productDTO) {
        // Uniqueness check
        if (productDTO.getProductId() == null) {
            if (productRepository.existsBySku(productDTO.getSku())) {
                throw new IllegalArgumentException("SKU already exists.");
            }
        } else {
            if (productRepository.existsBySkuAndProductIdNot(productDTO.getSku(), productDTO.getProductId())) {
                throw new IllegalArgumentException("SKU already exists.");
            }
        }

        Product product;
        if (productDTO.getProductId() != null) {
            // Update
            product = productRepository.findById(productDTO.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productDTO.getProductId()));
            product.setSku(productDTO.getSku());
            product.setProductName(productDTO.getProductName());
            product.setUnitWeight(productDTO.getUnitWeight());
            product.setBaseUoM(productDTO.getBaseUoM());
            product.setMinStockLevel(productDTO.getMinStockLevel());
        } else {
            // Create
            product = Product.builder()
                    .sku(productDTO.getSku())
                    .productName(productDTO.getProductName())
                    .unitWeight(productDTO.getUnitWeight())
                    .baseUoM(productDTO.getBaseUoM())
                    .minStockLevel(productDTO.getMinStockLevel())
                    .isActive(true)
                    .build();
        }

        return productRepository.save(product);
    }

    @Override
    public void toggleActive(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        product.setIsActive(!product.getIsActive());
        productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> findPaginated(String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("productName").ascending());
        if (keyword != null && !keyword.isBlank()) {
            return productRepository.searchByKeywordPageable(keyword.trim(), pageable);
        }
        return productRepository.findAll(pageable);
    }
}
