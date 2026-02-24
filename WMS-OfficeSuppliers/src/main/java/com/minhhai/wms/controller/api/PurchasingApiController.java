package com.minhhai.wms.controller.api;

import com.minhhai.wms.entity.Product;
import com.minhhai.wms.entity.ProductUoMConversion;
import com.minhhai.wms.repository.ProductRepository;
import com.minhhai.wms.repository.ProductUoMConversionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/purchasing")
@RequiredArgsConstructor
public class PurchasingApiController {

    private final ProductRepository productRepository;
    private final ProductUoMConversionRepository uomConversionRepository;

    @GetMapping("/products/{productId}/uoms")
    public ResponseEntity<List<String>> getProductUoMs(@PathVariable("productId") Integer productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Product product = productOpt.get();
        List<String> uoms = new ArrayList<>();
        
        // Base UoM is always an option
        uoms.add(product.getBaseUoM());
        
        // Add defined conversion UoMs
        List<ProductUoMConversion> conversions = uomConversionRepository.findByProduct_ProductId(product.getProductId());
        for (ProductUoMConversion conversion : conversions) {
            uoms.add(conversion.getFromUoM());
        }
        
        return ResponseEntity.ok(uoms);
    }
}
