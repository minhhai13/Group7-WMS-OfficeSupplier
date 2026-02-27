package com.minhhai.wms.service.impl;

import com.minhhai.wms.entity.Product;
import com.minhhai.wms.entity.ProductUoMConversion;
import com.minhhai.wms.repository.ProductUoMConversionRepository;
import com.minhhai.wms.service.ProductUoMConversionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductUoMConversionServiceImpl implements ProductUoMConversionService {

    private final ProductUoMConversionRepository conversionRepository;
    private final com.minhhai.wms.repository.ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductUoMConversion> findByProductId(Integer productId) {
        return conversionRepository.findByProduct_ProductId(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductUoMConversion> findById(Integer id) {
        return conversionRepository.findById(id);
    }

    @Override
    public ProductUoMConversion save(ProductUoMConversion conversion) {
        return conversionRepository.save(conversion);
    }

    @Override
    public ProductUoMConversion save(com.minhhai.wms.dto.ProductUoMConversionDTO dto) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + dto.getProductId()));

        String baseUoM = product.getBaseUoM();
        String fromUoM = dto.getFromUoM().trim();

        if (fromUoM.equalsIgnoreCase(baseUoM)) {
            throw new IllegalArgumentException("From UoM cannot be the same as the product's Base UoM (" + baseUoM + ").");
        }
        if (dto.getConversionId() == null) {
            if (conversionRepository.existsByProductProductIdAndFromUoMAndToUoM(dto.getProductId(), dto.getFromUoM(), dto.getToUoM())) {
                throw new IllegalArgumentException("This conversion already exists for the product.");
            }
        } else {
            if (conversionRepository.existsByProductProductIdAndFromUoMAndToUoMAndConversionIdNot(dto.getProductId(), dto.getFromUoM(), dto.getToUoM(), dto.getConversionId())) {
                throw new IllegalArgumentException("This conversion already exists for the product.");
            }
        }

        ProductUoMConversion conversion;
        if (dto.getConversionId() != null) {
            // Update
            conversion = conversionRepository.findById(dto.getConversionId())
                    .orElseThrow(() -> new IllegalArgumentException("Conversion not found: " + dto.getConversionId()));
            conversion.setFromUoM(fromUoM);
            conversion.setToUoM(baseUoM);
            conversion.setConversionFactor(dto.getConversionFactor());
        } else {
            // Create
            conversion = ProductUoMConversion.builder()
                    .product(product)
                    .fromUoM(fromUoM)
                    .toUoM(baseUoM)
                    .conversionFactor(dto.getConversionFactor())
                    .build();
        }

        return conversionRepository.save(conversion);
    }

    @Override
    public void delete(Integer conversionId) {
        conversionRepository.deleteById(conversionId);
    }
}
