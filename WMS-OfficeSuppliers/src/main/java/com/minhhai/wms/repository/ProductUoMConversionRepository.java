package com.minhhai.wms.repository;

import com.minhhai.wms.entity.ProductUoMConversion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductUoMConversionRepository extends JpaRepository<ProductUoMConversion, Integer> {

    List<ProductUoMConversion> findByProduct_ProductId(Integer productId);

    boolean existsByProductProductIdAndFromUoMAndToUoM(Integer productId, String fromUoM, String toUoM);

    boolean existsByProductProductIdAndFromUoMAndToUoMAndConversionIdNot(
            Integer productId, String fromUoM, String toUoM, Integer conversionId);
}
