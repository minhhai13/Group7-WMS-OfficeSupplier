package com.minhhai.wms.dao;

import com.minhhai.wms.entity.ProductUoMConversion;

import java.util.List;
import java.util.Optional;

public interface ProductUoMConversionDao {

    List<ProductUoMConversion> findByProductId(Integer productId);

    Optional<ProductUoMConversion> findById(Integer id);

    boolean existsByProductIdAndFromUoMAndToUoM(Integer productId, String fromUoM, String toUoM);

    boolean existsByProductIdAndFromUoMAndToUoMAndConversionIdNot(
            Integer productId, String fromUoM, String toUoM, Integer conversionId);

    ProductUoMConversion save(ProductUoMConversion conversion);

    void deleteById(Integer conversionId);
}
