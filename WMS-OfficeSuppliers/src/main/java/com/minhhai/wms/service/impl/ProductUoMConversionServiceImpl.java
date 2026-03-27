package com.minhhai.wms.service.impl;

import com.minhhai.wms.dao.ProductDao;
import com.minhhai.wms.dao.ProductUoMConversionDao;
import com.minhhai.wms.entity.Product;
import com.minhhai.wms.entity.ProductUoMConversion;
import com.minhhai.wms.service.ProductUoMConversionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductUoMConversionServiceImpl implements ProductUoMConversionService {

    private final ProductUoMConversionDao conversionDao;
    private final ProductDao productDao;

    @Override
    @Transactional(readOnly = true)
    public List<ProductUoMConversion> findByProductId(Integer productId) {
        return conversionDao.findByProductId(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductUoMConversion> findById(Integer id) {
        return conversionDao.findById(id);
    }

    @Override
    public ProductUoMConversion save(ProductUoMConversion conversion) {
        return conversionDao.save(conversion);
    }

    @Override
    public ProductUoMConversion save(com.minhhai.wms.dto.ProductUoMConversionDTO dto) {
        Product product = productDao.findById(dto.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + dto.getProductId()));

        String baseUoM = product.getBaseUoM();
        String fromUoM = dto.getFromUoM().trim();

        if (fromUoM.equalsIgnoreCase(baseUoM)) {
            throw new IllegalArgumentException("From UoM cannot be the same as the product's Base UoM (" + baseUoM + ").");
        }
        if (dto.getConversionId() == null) {
            if (conversionDao.existsByProductIdAndFromUoMAndToUoM(dto.getProductId(), dto.getFromUoM(), dto.getToUoM())) {
                throw new IllegalArgumentException("This conversion already exists for the product.");
            }
        } else {
            if (conversionDao.existsByProductIdAndFromUoMAndToUoMAndConversionIdNot(dto.getProductId(), dto.getFromUoM(), dto.getToUoM(), dto.getConversionId())) {
                throw new IllegalArgumentException("This conversion already exists for the product.");
            }
        }

        ProductUoMConversion conversion;
        if (dto.getConversionId() != null) {
            conversion = conversionDao.findById(dto.getConversionId())
                    .orElseThrow(() -> new IllegalArgumentException("Conversion not found: " + dto.getConversionId()));
            conversion.setFromUoM(fromUoM);
            conversion.setToUoM(baseUoM);
            conversion.setConversionFactor(dto.getConversionFactor());
        } else {
            conversion = ProductUoMConversion.builder()
                    .product(product)
                    .fromUoM(fromUoM)
                    .toUoM(baseUoM)
                    .conversionFactor(dto.getConversionFactor())
                    .build();
        }

        return conversionDao.save(conversion);
    }

    @Override
    public void delete(Integer conversionId) {
        conversionDao.deleteById(conversionId);
    }

    @Override
    public List<Map<String, String>> getAvailableUoMs(Integer productId) {
        Product product = productDao.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        List<Map<String, String>> uoms = new ArrayList<>();

        // 1. Thêm đơn vị cơ bản
        Map<String, String> base = new HashMap<>();
        base.put("uom", product.getBaseUoM());
        base.put("display", product.getBaseUoM() + " (Base)");
        uoms.add(base);

        // 2. Thêm các đơn vị chuyển đổi
        List<ProductUoMConversion> conversions = conversionDao.findByProductId(productId);
        for (ProductUoMConversion conv : conversions) {
            Map<String, String> m = new HashMap<>();
            m.put("uom", conv.getFromUoM());
            m.put("display", conv.getFromUoM() + " (x" + conv.getConversionFactor() + ")");
            uoms.add(m);
        }
        return uoms;
    }
}
