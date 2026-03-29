package com.minhhai.wms.service.impl;

import com.minhhai.wms.dao.StockBatchDao;
import com.minhhai.wms.dao.StockMovementDao;
import com.minhhai.wms.dto.CurrentStockDTO;
import com.minhhai.wms.dto.InboundReportDTO;
import com.minhhai.wms.dto.InventoryBalanceDTO;
import com.minhhai.wms.dto.OutboundReportDTO;
import com.minhhai.wms.entity.StockMovement;
import com.minhhai.wms.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final StockMovementDao stockMovementDao;
    private final StockBatchDao stockBatchDao;

    // ==================== Inbound ====================

    @Override
    public Page<InboundReportDTO> getInboundReport(
            LocalDate startDate, LocalDate endDate,
            Integer warehouseId, Integer productId,
            int page, int size) {

        LocalDateTime startDt = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDt   = (endDate   != null) ? endDate.atTime(LocalTime.MAX) : null;

        return stockMovementDao
                .findInboundHistory(startDt, endDt, warehouseId, productId, PageRequest.of(page, size))
                .map(this::mapToInboundDTO);
    }

    // ==================== Outbound ====================

    @Override
    public Page<OutboundReportDTO> getOutboundReport(
            LocalDate startDate, LocalDate endDate,
            Integer warehouseId, Integer productId,
            int page, int size) {

        LocalDateTime startDt = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDt   = (endDate   != null) ? endDate.atTime(LocalTime.MAX) : null;

        return stockMovementDao
                .findOutboundHistory(startDt, endDt, warehouseId, productId, PageRequest.of(page, size))
                .map(this::mapToOutboundDTO);
    }

    // ==================== Inventory Balance ====================

    @Override
    public List<InventoryBalanceDTO> getInventoryReport(
            LocalDate startDate, LocalDate endDate, Integer warehouseId) {

        LocalDate resolvedStart = (startDate != null) ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate resolvedEnd   = (endDate   != null) ? endDate   : LocalDate.now();

        LocalDateTime beforeDt = resolvedStart.atStartOfDay();
        LocalDateTime startDt  = resolvedStart.atStartOfDay();
        LocalDateTime endDt    = resolvedEnd.atTime(LocalTime.MAX);

        // Query 1: Opening stock (Receipt - Issue) before startDate
        List<Object[]> openingRows = stockMovementDao.findOpeningStock(beforeDt, warehouseId);

        // Query 2: Period summary (inbound + outbound) in [startDate, endDate]
        List<Object[]> periodRows  = stockMovementDao.findPeriodSummary(startDt, endDt, warehouseId);

        // Build result map keyed by "productId-warehouseId"
        Map<String, InventoryBalanceDTO> resultMap = new LinkedHashMap<>();

        for (Object[] row : openingRows) {
            Integer productId  = (Integer) row[0];
            String  sku        = (String)  row[1];
            String  productName= (String)  row[2];
            Integer warehId    = (Integer) row[3];
            String  warehName  = (String)  row[4];
            String  uom        = (String)  row[5];
            int     opening    = ((Number) row[6]).intValue();

            String key = productId + "-" + warehId;
            resultMap.put(key, InventoryBalanceDTO.builder()
                    .productId(productId).sku(sku).productName(productName)
                    .warehouseId(warehId).warehouseName(warehName).uom(uom)
                    .openingStock(opening).inboundQty(0).outboundQty(0)
                    .closingStock(opening)
                    .build());
        }

        for (Object[] row : periodRows) {
            Integer productId  = (Integer) row[0];
            String  sku        = (String)  row[1];
            String  pName      = (String)  row[2];
            Integer warehId    = (Integer) row[3];
            String  wName      = (String)  row[4];
            String  uom        = (String)  row[5];
            int     inbound    = ((Number) row[6]).intValue();
            int     outbound   = ((Number) row[7]).intValue();

            String key = productId + "-" + warehId;
            InventoryBalanceDTO dto = resultMap.get(key);
            if (dto == null) {
                dto = InventoryBalanceDTO.builder()
                        .productId(productId).sku(sku).productName(pName)
                        .warehouseId(warehId).warehouseName(wName).uom(uom)
                        .openingStock(0).inboundQty(0).outboundQty(0)
                        .build();
                resultMap.put(key, dto);
            }
            dto.setInboundQty(inbound);
            dto.setOutboundQty(outbound);
            dto.setClosingStock(dto.getOpeningStock() + inbound - outbound);
        }

        return resultMap.values().stream()
                .sorted(Comparator.comparing(d ->
                        (d.getWarehouseName() != null ? d.getWarehouseName() : "") +
                        (d.getProductName()   != null ? d.getProductName()   : "")))
                .collect(Collectors.toList());
    }

    // ==================== Mapping helpers ====================

    private InboundReportDTO mapToInboundDTO(StockMovement m) {
        return InboundReportDTO.builder()
                .movementDate(m.getMovementDate())
                .warehouseName(m.getWarehouse() != null ? m.getWarehouse().getWarehouseName() : null)
                .productName(m.getProduct()   != null ? m.getProduct().getProductName() : null)
                .sku(m.getProduct()           != null ? m.getProduct().getSku() : null)
                .batchNumber(m.getBatchNumber())
                .binLocation(m.getBin()       != null ? m.getBin().getBinLocation() : null)
                .quantity(m.getQuantity())
                .uom(m.getUom())
                .balanceAfter(m.getBalanceAfter())
                .build();
    }

    private OutboundReportDTO mapToOutboundDTO(StockMovement m) {
        return OutboundReportDTO.builder()
                .movementDate(m.getMovementDate())
                .warehouseName(m.getWarehouse() != null ? m.getWarehouse().getWarehouseName() : null)
                .productName(m.getProduct()   != null ? m.getProduct().getProductName() : null)
                .sku(m.getProduct()           != null ? m.getProduct().getSku() : null)
                .batchNumber(m.getBatchNumber())
                .binLocation(m.getBin()       != null ? m.getBin().getBinLocation() : null)
                .quantity(m.getQuantity())
                .uom(m.getUom())
                .balanceAfter(m.getBalanceAfter())
                .build();
    }

    // ==================== Current Stock ====================

    @Override
    public List<CurrentStockDTO> getCurrentStock(Integer warehouseId, boolean lowStockOnly) {
        return stockBatchDao.findCurrentStockByWarehouse(warehouseId, lowStockOnly);
    }

    @Override
    public long countLowStockProducts(Integer warehouseId) {
        return stockBatchDao.countLowStockProducts(warehouseId);
    }
}
