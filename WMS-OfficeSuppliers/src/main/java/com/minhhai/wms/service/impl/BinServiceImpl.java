package com.minhhai.wms.service.impl;

import com.minhhai.wms.entity.Bin;
import com.minhhai.wms.entity.StockBatch;
import com.minhhai.wms.entity.Warehouse;
import com.minhhai.wms.repository.BinRepository;
import com.minhhai.wms.repository.StockBatchRepository;
import com.minhhai.wms.service.BinService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class BinServiceImpl implements BinService {

    private final BinRepository binRepository;
    private final StockBatchRepository stockBatchRepository;
    private final com.minhhai.wms.repository.WarehouseRepository warehouseRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Bin> findByWarehouseId(Integer warehouseId) {
        return binRepository.findByWarehouseWarehouseId(warehouseId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Bin> findById(Integer id) {
        return binRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bin> search(Integer warehouseId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return findByWarehouseId(warehouseId);
        }
        return binRepository.searchInWarehouse(warehouseId, keyword.trim());
    }

    @Override
    public Bin save(Bin bin) {
        return binRepository.save(bin);
    }

    @Override
    public Bin save(com.minhhai.wms.dto.BinDTO binDTO) {
        // Uniqueness check
        if (binDTO.getBinId() == null) {
            if (binRepository.existsByWarehouseWarehouseIdAndBinLocation(binDTO.getWarehouseId(), binDTO.getBinLocation())) {
                throw new IllegalArgumentException("Bin location already exists in this warehouse.");
            }
        } else {
            if (binRepository.existsByWarehouseWarehouseIdAndBinLocationAndBinIdNot(binDTO.getWarehouseId(), binDTO.getBinLocation(), binDTO.getBinId())) {
                throw new IllegalArgumentException("Bin location already exists in this warehouse.");
            }
        }

        Warehouse warehouse = warehouseRepository.findById(binDTO.getWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + binDTO.getWarehouseId()));

        Bin bin;
        if (binDTO.getBinId() != null) {
            // Update
            bin = binRepository.findById(binDTO.getBinId())
                    .orElseThrow(() -> new IllegalArgumentException("Bin not found: " + binDTO.getBinId()));
            bin.setBinLocation(binDTO.getBinLocation());
            BigDecimal currentWeight = getCurrentWeight(binDTO.getBinId());
            if (binDTO.getMaxWeight().compareTo(currentWeight) < 0) {
                throw new IllegalArgumentException("New max weight (" + binDTO.getMaxWeight() +
                        " kg) can not be smaller than current weight (" + currentWeight + " kg).");
            }
            bin.setMaxWeight(binDTO.getMaxWeight());
            // Warehouse usually doesn't change for a bin, but if it does:
            bin.setWarehouse(warehouse);
        } else {
            // Create
            bin = Bin.builder()
                    .binLocation(binDTO.getBinLocation())
                    .maxWeight(binDTO.getMaxWeight())
                    .warehouse(warehouse)
                    .isActive(true)
                    .build();
        }

        return binRepository.save(bin);
    }

    @Override
    public void toggleActive(Integer binId) {
        Bin bin = binRepository.findById(binId)
                .orElseThrow(() -> new RuntimeException("Bin not found: " + binId));
        if (bin.getIsActive()) {
            List<StockBatch> batches = stockBatchRepository.findByBinBinId(binId);
            int totalQty = batches.stream()
                    .mapToInt(b -> b.getQtyAvailable() != null ? b.getQtyAvailable() : 0)
                    .sum();
            if (totalQty > 0) {
                throw new IllegalArgumentException("Can not deactive this bin because it has " + totalQty + " product inside.");
            }
        }
        bin.setIsActive(!bin.getIsActive());
        binRepository.save(bin);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCurrentWeight(Integer binId) {
        BigDecimal totalWeight = stockBatchRepository.getTotalWeightByBinId(binId);

        // Nếu Bin trống (DB trả về null), thì trọng lượng là 0
        return totalWeight != null ? totalWeight : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAvailableCapacity(Integer binId) {
        Bin bin = binRepository.findById(binId)
                .orElseThrow(() -> new RuntimeException("Bin not found: " + binId));
        BigDecimal maxWeight = bin.getMaxWeight() != null ? bin.getMaxWeight() : BigDecimal.ZERO;
        BigDecimal currentWeight = getCurrentWeight(binId);
        BigDecimal available = maxWeight.subtract(currentWeight);
        return available.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : available;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Bin> findPaginated(Integer warehouseId, String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("binLocation").ascending());
        if (keyword != null && !keyword.isBlank()) {
            return binRepository.searchInWarehousePageable(warehouseId, keyword.trim(), pageable);
        }
        return binRepository.findByWarehouseWarehouseId(warehouseId, pageable);
    }
}
