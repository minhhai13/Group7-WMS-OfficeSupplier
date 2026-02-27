package com.minhhai.wms.service.impl;

import com.minhhai.wms.dto.InOutBalanceReportDTO;
import com.minhhai.wms.dto.InboundReportDTO;
import com.minhhai.wms.dto.OutboundReportDTO;
import com.minhhai.wms.dto.PhysicalInventoryDTO;
import com.minhhai.wms.entity.StockMovement;
import com.minhhai.wms.repository.*;
import com.minhhai.wms.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final StockBatchRepository stockBatchRepository;
    private final GoodsReceiptDetailRepository grnDetailRepository;
    private final GoodsIssueDetailRepository ginDetailRepository;
    private final StockMovementRepository stockMovementRepository;

    @Override
    public Page<PhysicalInventoryDTO> getPhysicalInventory(Integer warehouseId, Pageable pageable) {
        // Truyền pageable vào repository để DB xử lý LIMIT/OFFSET
        return stockBatchRepository.getPhysicalInventoryByWarehouse(warehouseId, pageable);
    }

    @Override
    public Page<InboundReportDTO> getInboundReport(Integer warehouseId, Pageable pageable) {
        return grnDetailRepository.getInboundReportByWarehouse(warehouseId, pageable);
    }

    @Override
    public Page<OutboundReportDTO> getOutboundReport(Integer warehouseId, Pageable pageable) {
        return ginDetailRepository.getOutboundReportByWarehouse(warehouseId, pageable);
    }

    // =========================================================================
    // UC30: IN-OUT-BALANCE REPORT (Thẻ kho)
    // =========================================================================

    @Override
    public List<InOutBalanceReportDTO> getInOutBalanceReport(Integer warehouseId, Integer productId) {
        List<StockMovement> movements = stockMovementRepository.getMovementsByProductAndWarehouse(productId, warehouseId);

        // Sử dụng Stream API để map dữ liệu, code gọn gàng và dễ đọc hơn rất nhiều
        return movements.stream()
                .map(this::buildBalanceDTO)
                .collect(Collectors.toList());
    }

    // --- HELPER METHODS CHO BÁO CÁO THẺ KHO ---

    private InOutBalanceReportDTO buildBalanceDTO(StockMovement sm) {
        int qty = sm.getQuantity() != null ? sm.getQuantity() : 0;
        int closingBalance = sm.getBalanceAfter() != null ? sm.getBalanceAfter() : 0;
        String type = sm.getMovementType();

        // Tách bạch logic tính toán ra các hàm chuyên trách
        int inboundQty = calculateInboundQty(type, qty);
        int outboundQty = calculateOutboundQty(type, qty);
        int openingBalance = calculateOpeningBalance(closingBalance, inboundQty, outboundQty);
        String binLoc = (sm.getBin() != null) ? sm.getBin().getBinLocation() : "N/A";

        return new InOutBalanceReportDTO(
                sm.getMovementDate(), type, inboundQty, outboundQty,
                openingBalance, closingBalance, binLoc, sm.getBatchNumber()
        );
    }

    private int calculateInboundQty(String type, int qty) {
        return ("Receipt".equalsIgnoreCase(type) || "Transfer-In".equalsIgnoreCase(type)) ? qty : 0;
    }

    private int calculateOutboundQty(String type, int qty) {
        return ("Issue".equalsIgnoreCase(type) || "Transfer-Out".equalsIgnoreCase(type)) ? qty : 0;
    }

    private int calculateOpeningBalance(int closingBalance, int inboundQty, int outboundQty) {
        // Logic tính lùi: Tồn đầu = Tồn cuối - Nhập + Xuất
        if (inboundQty > 0) {
            return closingBalance - inboundQty;
        }
        if (outboundQty > 0) {
            return closingBalance + outboundQty;
        }
        return closingBalance; // Không biến động vật lý (VD: Giữ chỗ - Reserve)
    }
}