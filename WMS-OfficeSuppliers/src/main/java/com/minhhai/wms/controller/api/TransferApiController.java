package com.minhhai.wms.controller.api;

import com.minhhai.wms.entity.Bin;
import com.minhhai.wms.entity.StockBatch;
import com.minhhai.wms.repository.StockBatchRepository;
import com.minhhai.wms.service.BinService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transfer")
@RequiredArgsConstructor
public class TransferApiController {

    private final StockBatchRepository stockBatchRepository;
    private final BinService binService;

    /**
     * GET /api/transfer/bins/{binId}/stock-batches
     * Trả về danh sách product-batch có hàng (qtyAvailable > 0) trong một bin.
     */
    @Transactional(readOnly = true)
    @GetMapping("/bins/{binId}/stock-batches")
    public ResponseEntity<List<Map<String, Object>>> getStockBatchesByBin(@PathVariable Integer binId) {
        List<StockBatch> batches = stockBatchRepository.findByBinBinIdEager(binId)
                .stream()
                .filter(sb -> sb.getQtyAvailable() != null && sb.getQtyAvailable() > 0)
                .collect(Collectors.toList());

        List<Map<String, Object>> result = batches.stream().map(sb -> Map.<String, Object>of(
                "stockBatchId", sb.getStockBatchId(),
                "productId",    sb.getProduct().getProductId(),
                "productName",  sb.getProduct().getSku() + " - " + sb.getProduct().getProductName(),
                "batchNumber",  sb.getBatchNumber(),
                "qtyAvailable", sb.getQtyAvailable(),
                "uom",          sb.getUom()
        )).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/transfer/warehouses/{warehouseId}/bins
     * Trả về tất cả các bin còn active trong kho (làm To Bin).
     * Có thể loại trừ fromBinId nếu truyền query param exclude.
     */
    @Transactional(readOnly = true)
    @GetMapping("/warehouses/{warehouseId}/bins")
    public ResponseEntity<List<Map<String, Object>>> getBinsByWarehouse(
            @PathVariable Integer warehouseId,
            @RequestParam(required = false) Integer excludeBinId) {

        List<Bin> bins = binService.findByWarehouseId(warehouseId)
                .stream()
                .filter(b -> Boolean.TRUE.equals(b.getIsActive()))
                .filter(b -> excludeBinId == null || !b.getBinId().equals(excludeBinId))
                .collect(Collectors.toList());

        List<Map<String, Object>> result = bins.stream().map(b -> Map.<String, Object>of(
                "binId",       b.getBinId(),
                "binLocation", b.getBinLocation()
        )).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
