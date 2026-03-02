package com.minhhai.wms.controller.api;

import com.minhhai.wms.service.SalesOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SalesApiController {

    private final SalesOrderService soService;

    @GetMapping("/products/{id}/uoms")
    public ResponseEntity<List<Map<String, String>>> getProductUoMs(@PathVariable(name = "id") Integer productId) {
        try {
            List<Map<String, String>> uoms = soService.getAvailableUoMs(productId);
            return ResponseEntity.ok(uoms);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}