package com.minhhai.wms.service.impl;

import com.minhhai.wms.entity.Warehouse;
import com.minhhai.wms.repository.WarehouseRepository;
import com.minhhai.wms.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Warehouse> findAll() {
        return warehouseRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Warehouse> findAllActive() {
        return warehouseRepository.findByIsActive(true);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Warehouse> findById(Integer id) {
        return warehouseRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Warehouse> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }
        return warehouseRepository.searchByKeyword(keyword.trim());
    }

    @Override
    public Warehouse save(Warehouse warehouse) {
        return warehouseRepository.save(warehouse);
    }

    @Override
    public Warehouse save(com.minhhai.wms.dto.WarehouseDTO warehouseDTO) {
        // Uniqueness check
        if (warehouseDTO.getWarehouseId() == null) {
            if (warehouseRepository.existsByWarehouseCode(warehouseDTO.getWarehouseCode())) {
                throw new IllegalArgumentException("Warehouse code already exists.");
            }
        } else {
            if (warehouseRepository.existsByWarehouseCodeAndWarehouseIdNot(warehouseDTO.getWarehouseCode(), warehouseDTO.getWarehouseId())) {
                throw new IllegalArgumentException("Warehouse code already exists.");
            }
        }

        Warehouse warehouse;
        if (warehouseDTO.getWarehouseId() != null) {
            // Update
            warehouse = warehouseRepository.findById(warehouseDTO.getWarehouseId())
                    .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + warehouseDTO.getWarehouseId()));
            warehouse.setWarehouseCode(warehouseDTO.getWarehouseCode());
            warehouse.setWarehouseName(warehouseDTO.getWarehouseName());
            warehouse.setAddress(warehouseDTO.getAddress());
        } else {
            // Create
            warehouse = Warehouse.builder()
                    .warehouseCode(warehouseDTO.getWarehouseCode())
                    .warehouseName(warehouseDTO.getWarehouseName())
                    .address(warehouseDTO.getAddress())
                    .isActive(true)
                    .build();
        }

        return warehouseRepository.save(warehouse);
    }

    @Override
    public void toggleActive(Integer warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new RuntimeException("Warehouse not found: " + warehouseId));
        warehouse.setIsActive(!warehouse.getIsActive());
        warehouseRepository.save(warehouse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Warehouse> findAllActiveExcluding(Integer warehouseId) {
        return warehouseRepository.findByIsActiveTrueAndWarehouseIdNot(warehouseId);
    }
}
