package com.minhhai.wms.service.impl;

import com.minhhai.wms.dao.BinDao;
import com.minhhai.wms.dao.StockBatchDao;
import com.minhhai.wms.dao.WarehouseDao;
import com.minhhai.wms.entity.Bin;
import com.minhhai.wms.entity.Warehouse;
import com.minhhai.wms.service.BinService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class BinServiceImpl implements BinService {

    private final BinDao binDao;
    private final StockBatchDao stockBatchDao;
    private final WarehouseDao warehouseDao;

    @Override
    @Transactional(readOnly = true)
    public List<Bin> findByWarehouseId(Integer warehouseId) {
        return binDao.findByWarehouseId(warehouseId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Bin> findById(Integer id) {
        return binDao.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Bin> search(Integer warehouseId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return findByWarehouseId(warehouseId);
        }
        return binDao.searchInWarehouse(warehouseId, keyword.trim());
    }

    @Override
    public Bin save(Bin bin) {
        return binDao.save(bin);
    }

    @Override
    public Bin save(com.minhhai.wms.dto.BinDTO binDTO) {
        // Uniqueness check
        if (binDTO.getBinId() == null) {
            if (binDao.existsByWarehouseIdAndBinLocation(binDTO.getWarehouseId(), binDTO.getBinLocation())) {
                throw new IllegalArgumentException("Bin location already exists in this warehouse.");
            }
        } else {
            if (binDao.existsByWarehouseIdAndBinLocationAndBinIdNot(binDTO.getWarehouseId(), binDTO.getBinLocation(), binDTO.getBinId())) {
                throw new IllegalArgumentException("Bin location already exists in this warehouse.");
            }
        }

        Warehouse warehouse = warehouseDao.findById(binDTO.getWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + binDTO.getWarehouseId()));

        Bin bin;
        if (binDTO.getBinId() != null) {
            bin = binDao.findById(binDTO.getBinId())
                    .orElseThrow(() -> new IllegalArgumentException("Bin not found: " + binDTO.getBinId()));
            bin.setBinLocation(binDTO.getBinLocation());
            BigDecimal currentWeight = getCurrentWeight(binDTO.getBinId());
            if (binDTO.getMaxWeight().compareTo(currentWeight) < 0) {
                throw new IllegalArgumentException("New max weight (" + binDTO.getMaxWeight() +
                        " kg) can not be smaller than current weight (" + currentWeight + " kg).");
            }
            bin.setMaxWeight(binDTO.getMaxWeight());
            bin.setWarehouse(warehouse);
        } else {
            bin = Bin.builder()
                    .binLocation(binDTO.getBinLocation())
                    .maxWeight(binDTO.getMaxWeight())
                    .warehouse(warehouse)
                    .isActive(true)
                    .build();
        }

        return binDao.save(bin);
    }

    @Override
    public void toggleActive(Integer binId) {
        Bin bin = binDao.findById(binId)
                .orElseThrow(() -> new RuntimeException("Bin not found: " + binId));
        if (bin.getIsActive()) {
            int totalQty = stockBatchDao.getTotalQtyAvailableByBinId(binId);
            if (totalQty > 0) {
                throw new IllegalArgumentException("Can not deactive this bin because it has " + totalQty + " product inside.");
            }
        }
        bin.setIsActive(!bin.getIsActive());
        binDao.save(bin);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCurrentWeight(Integer binId) {
        return stockBatchDao.getTotalWeightByBinId(binId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getAvailableCapacity(Integer binId) {
        Bin bin = binDao.findById(binId)
                .orElseThrow(() -> new RuntimeException("Bin not found: " + binId));
        BigDecimal maxWeight = bin.getMaxWeight() != null ? bin.getMaxWeight() : BigDecimal.ZERO;
        BigDecimal currentWeight = getCurrentWeight(binId);
        BigDecimal available = maxWeight.subtract(currentWeight);
        return available.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : available;
    }
}
