package com.minhhai.wms.service;

import com.minhhai.wms.dto.InOutBalanceReportDTO;
import com.minhhai.wms.dto.InboundReportDTO;
import com.minhhai.wms.dto.OutboundReportDTO;
import com.minhhai.wms.dto.PhysicalInventoryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReportService {
    // Các báo cáo hỗ trợ phân trang
    Page<PhysicalInventoryDTO> getPhysicalInventory(Integer warehouseId, Pageable pageable);
    Page<InboundReportDTO> getInboundReport(Integer warehouseId, Pageable pageable);
    Page<OutboundReportDTO> getOutboundReport(Integer warehouseId, Pageable pageable);

    // Thẻ kho giữ nguyên List để tính toán lũy kế
    List<InOutBalanceReportDTO> getInOutBalanceReport(Integer warehouseId, Integer productId);
}