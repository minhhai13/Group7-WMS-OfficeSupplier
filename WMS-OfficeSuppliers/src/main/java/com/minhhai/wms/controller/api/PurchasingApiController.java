package com.minhhai.wms.controller.api;

import com.minhhai.wms.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/purchasing")
@RequiredArgsConstructor
public class PurchasingApiController {

    private final PurchaseOrderService poService;

    /**
     * Returns available UoMs for a product (BaseUoM + conversion FromUoMs).
     * Called via AJAX from the PO form when a product is selected.
     */
    @GetMapping("/products/{id}/uoms")
    public ResponseEntity<List<Map<String, String>>> getProductUoMs(@PathVariable(name = "id") Integer productId) {
        try {
            List<Map<String, String>> uoms = poService.getAvailableUoMs(productId);
            return ResponseEntity.ok(uoms);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
