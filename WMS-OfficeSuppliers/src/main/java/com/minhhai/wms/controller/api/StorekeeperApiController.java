package com.minhhai.wms.controller.api;

import com.minhhai.wms.dto.BinDTO;
import com.minhhai.wms.entity.Bin;
import com.minhhai.wms.entity.StockBatch;
import com.minhhai.wms.repository.BinRepository;
import com.minhhai.wms.repository.StockBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/storekeeper")
@RequiredArgsConstructor
public class StorekeeperApiController {

    private final BinRepository binRepository;
    private final StockBatchRepository stockBatchRepository;

    @GetMapping("/warehouse/{warehouseId}/available-bins")
    public ResponseEntity<List<BinDTO>> getAvailableBins(@PathVariable Integer warehouseId) {
        List<Bin> bins = binRepository.findByWarehouseWarehouseIdAndIsActiveTrue(warehouseId);
        List<BinDTO> availableBins = new ArrayList<>();
        
        for (Bin bin : bins) {
            BigDecimal currentWeight = calculateCurrentWeightInBin(bin.getBinId());
            BigDecimal availableWeight = bin.getMaxWeight().subtract(currentWeight);
            
            if (availableWeight.compareTo(BigDecimal.ZERO) > 0) {
                BinDTO dto = new BinDTO();
                dto.setBinId(bin.getBinId());
                dto.setBinLocation(bin.getBinLocation());
                dto.setMaxWeight(bin.getMaxWeight());
                // We're hijacking this DTO to return available weight to the frontend
                // This assumes we have a generic way to pass it, or we can just send maxWeight as availableWeight
                // For safety we'll use maxWeight property as availableWeight to display
                dto.setMaxWeight(availableWeight);
                availableBins.add(dto);
            }
        }
        
        return ResponseEntity.ok(availableBins);
    }

    private BigDecimal calculateCurrentWeightInBin(Integer binId) {
        List<StockBatch> batches = stockBatchRepository.findByBinBinId(binId);
        BigDecimal total = BigDecimal.ZERO;
        for (StockBatch b : batches) {
            BigDecimal qty = new BigDecimal(b.getQtyAvailable() + b.getQtyReserved());
            total = total.add(b.getProduct().getUnitWeight().multiply(qty));
        }
        return total;
    }
}
