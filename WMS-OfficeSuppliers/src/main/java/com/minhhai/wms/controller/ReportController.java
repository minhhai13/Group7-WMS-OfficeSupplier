package com.minhhai.wms.controller;

import com.minhhai.wms.dto.InboundReportDTO;
import com.minhhai.wms.dto.InventoryBalanceDTO;
import com.minhhai.wms.dto.OutboundReportDTO;
import com.minhhai.wms.entity.User;
import com.minhhai.wms.service.ProductService;
import com.minhhai.wms.service.ReportService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ProductService productService;

    private static final int PAGE_SIZE = 20;

    /**
     * UC28: Báo cáo lịch sử nhập hàng của kho quản lý
     */
    @GetMapping("/inbound")
    public String inboundHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer productId,
            @RequestParam(defaultValue = "0") int page,
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("loggedInUser");
        // Lấy trực tiếp ID kho của Manager, không nhận từ tham số bên ngoài
        Integer warehouseId = user.getWarehouse().getWarehouseId();

        Page<InboundReportDTO> resultPage = reportService.getInboundReport(
                startDate, endDate, warehouseId, productId, page, PAGE_SIZE);

        model.addAttribute("resultPage", resultPage);
        model.addAttribute("rows", resultPage.getContent());
        model.addAttribute("products", productService.findAllActive());

        // Trả về thông tin kho hiện tại để hiển thị tiêu đề trên giao diện
        model.addAttribute("currentWarehouse", user.getWarehouse());

        model.addAttribute("selectedStartDate", startDate);
        model.addAttribute("selectedEndDate", endDate);
        model.addAttribute("selectedProductId", productId);
        model.addAttribute("activePage", "report-inbound");

        return "reports/inbound-history";
    }

    /**
     * UC29: Báo cáo lịch sử xuất hàng của kho quản lý
     */
    @GetMapping("/outbound")
    public String outboundHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer productId,
            @RequestParam(defaultValue = "0") int page,
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("loggedInUser");
        Integer warehouseId = user.getWarehouse().getWarehouseId();

        Page<OutboundReportDTO> resultPage = reportService.getOutboundReport(
                startDate, endDate, warehouseId, productId, page, PAGE_SIZE);

        model.addAttribute("resultPage", resultPage);
        model.addAttribute("rows", resultPage.getContent());
        model.addAttribute("products", productService.findAllActive());

        model.addAttribute("currentWarehouse", user.getWarehouse());

        model.addAttribute("selectedStartDate", startDate);
        model.addAttribute("selectedEndDate", endDate);
        model.addAttribute("selectedProductId", productId);
        model.addAttribute("activePage", "report-outbound");

        return "reports/outbound-history";
    }

    /**
     * UC27 & UC30: Báo cáo tồn kho thực tế (In-Out-Balance) của kho quản lý
     */
    @GetMapping("/inventory-balance")
    public String inventoryBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpSession session,
            Model model) {

        User user = (User) session.getAttribute("loggedInUser");
        Integer warehouseId = user.getWarehouse().getWarehouseId();

        // Mặc định khoảng thời gian nếu không chọn
        LocalDate finalStart = (startDate != null) ? startDate : LocalDate.now().withDayOfMonth(1);
        LocalDate finalEnd = (endDate != null) ? endDate : LocalDate.now();

        List<InventoryBalanceDTO> rows = reportService.getInventoryReport(finalStart, finalEnd, warehouseId);

        model.addAttribute("rows", rows);
        model.addAttribute("currentWarehouse", user.getWarehouse());
        model.addAttribute("selectedStartDate", finalStart);
        model.addAttribute("selectedEndDate", finalEnd);
        model.addAttribute("activePage", "report-inventory");

        return "reports/inventory-balance";
    }
}