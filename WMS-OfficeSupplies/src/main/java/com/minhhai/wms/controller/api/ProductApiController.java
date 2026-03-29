package com.minhhai.wms.controller.api;

import com.minhhai.wms.service.ProductUoMConversionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductApiController {
    private final ProductUoMConversionService uomService;

    @GetMapping("/{productId}/uoms")
    public ResponseEntity<List<Map<String, String>>> getUoMs(@PathVariable Integer productId) {
        try {
            List<Map<String, String>> uoms = uomService.getAvailableUoMs(productId);
            return ResponseEntity.ok(uoms);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}