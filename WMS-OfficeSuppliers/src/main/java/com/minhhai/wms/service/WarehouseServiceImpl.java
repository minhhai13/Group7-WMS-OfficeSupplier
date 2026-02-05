package com.minhhai.wms.service;

import com.minhhai.wms.entity.Warehouse;
import com.minhhai.wms.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Warehouse> findAll() {
        return warehouseRepository.findAllByOrderByWarehouseCodeAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Warehouse> findById(Integer id) {
        return warehouseRepository.findById(id);
    }

    @Override
    @Transactional
    public Warehouse save(Warehouse warehouse) {
        return warehouseRepository.save(warehouse);
    }

    @Override
    @Transactional
    public void deleteById(Integer id) {
        warehouseRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByWarehouseCode(String warehouseCode) {
        return warehouseRepository.existsByWarehouseCode(warehouseCode);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByWarehouseCodeExcludingId(String warehouseCode, Integer excludeWarehouseId) {
        return warehouseRepository.existsByWarehouseCodeAndWarehouseIdNot(warehouseCode, excludeWarehouseId);
    }
}
