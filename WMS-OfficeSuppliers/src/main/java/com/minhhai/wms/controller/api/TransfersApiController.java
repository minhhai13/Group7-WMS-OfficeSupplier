package com.minhhai.wms.controller.api;

import com.minhhai.wms.service.TransferRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transfer")
@RequiredArgsConstructor
public class TransfersApiController {

    private final TransferRequestService trService;

    @GetMapping("/products/{id}/uoms")
    public ResponseEntity<List<Map<String, String>>> getProductUoMs(@PathVariable(name = "id") Integer productId) {
        try {
            List<Map<String, String>> uoms = trService.getAvailableUoMs(productId);
            return ResponseEntity.ok(uoms);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
