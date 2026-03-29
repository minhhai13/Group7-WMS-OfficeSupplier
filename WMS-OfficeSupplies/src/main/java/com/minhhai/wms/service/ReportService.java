package com.minhhai.wms.service;

import com.minhhai.wms.dto.CurrentStockDTO;
import com.minhhai.wms.dto.InboundReportDTO;
import com.minhhai.wms.dto.InventoryBalanceDTO;
import com.minhhai.wms.dto.OutboundReportDTO;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    Page<InboundReportDTO> getInboundReport(LocalDate startDate, LocalDate endDate,
            Integer warehouseId, Integer productId, int page, int size);

    Page<OutboundReportDTO> getOutboundReport(LocalDate startDate, LocalDate endDate,
            Integer warehouseId, Integer productId, int page, int size);

    List<InventoryBalanceDTO> getInventoryReport(LocalDate startDate, LocalDate endDate, Integer warehouseId);

    /** Current real-time stock snapshot from StockBatches, grouped by product. */
    List<CurrentStockDTO> getCurrentStock(Integer warehouseId, boolean lowStockOnly);

    /** Count of products currently below MinStockLevel in the given warehouse. */
    long countLowStockProducts(Integer warehouseId);
}

