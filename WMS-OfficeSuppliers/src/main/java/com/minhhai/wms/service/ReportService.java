package com.minhhai.wms.service;

import com.minhhai.wms.dto.InboundReportDTO;
import com.minhhai.wms.dto.InventoryBalanceDTO;
import com.minhhai.wms.dto.OutboundReportDTO;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    /**
     * Returns paginated inbound history (Receipt / Physical movements only).
     */
    Page<InboundReportDTO> getInboundReport(
            LocalDate startDate,
            LocalDate endDate,
            Integer warehouseId,
            Integer productId,
            int page,
            int size);

    /**
     * Returns paginated outbound history (Issue / Physical movements only).
     */
    Page<OutboundReportDTO> getOutboundReport(
            LocalDate startDate,
            LocalDate endDate,
            Integer warehouseId,
            Integer productId,
            int page,
            int size);

    /**
     * Returns inventory balance per product+warehouse for a given period.
     * Opening = Sum(Receipt-Issue) before startDate
     * Closing = Opening + Inbound - Outbound
     */
    List<InventoryBalanceDTO> getInventoryReport(
            LocalDate startDate,
            LocalDate endDate,
            Integer warehouseId);
}
