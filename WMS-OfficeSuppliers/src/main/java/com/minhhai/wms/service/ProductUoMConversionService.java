package com.minhhai.wms.service;

import com.minhhai.wms.dto.ProductUoMConversionDTO;
import com.minhhai.wms.entity.ProductUoMConversion;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ProductUoMConversionService {

    List<ProductUoMConversion> findByProductId(Integer productId);

    Optional<ProductUoMConversion> findById(Integer id);

    ProductUoMConversion save(ProductUoMConversionDTO conversionDTO);

    ProductUoMConversion save(ProductUoMConversion conversion);

    void delete(Integer conversionId);


    List<Map<String, String>> getAvailableUoMs(Integer productId);
}