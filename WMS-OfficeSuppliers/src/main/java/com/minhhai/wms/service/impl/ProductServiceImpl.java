package com.minhhai.wms.service.impl;

import com.minhhai.wms.dto.ProductDTO;
import com.minhhai.wms.entity.Product;
import com.minhhai.wms.repository.ProductRepository;
import com.minhhai.wms.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    public List<ProductDTO> getAllProducts() {
        // Lấy tất cả Product từ DB và map sang DTO
        return productRepository.findAll().stream().map(product -> {
            ProductDTO dto = new ProductDTO();
            dto.setProductId(product.getProductId());
            dto.setSku(product.getSku());
            dto.setProductName(product.getProductName());

            // Bạn có thể set thêm các field khác nếu file ProductDTO của bạn yêu cầu (như baseUom, weight...)
            // Tuy nhiên với dropdown Báo cáo thì thường chỉ cần ID, SKU và Name là đủ.

            return dto;
        }).collect(Collectors.toList());
    }
}
