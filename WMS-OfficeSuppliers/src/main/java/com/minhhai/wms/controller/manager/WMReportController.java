package com.minhhai.wms.controller.manager;

import com.minhhai.wms.dto.InboundReportDTO;
import com.minhhai.wms.dto.OutboundReportDTO;
import com.minhhai.wms.dto.PhysicalInventoryDTO;
import com.minhhai.wms.service.ProductService;
import com.minhhai.wms.service.ReportService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/wm/reports")
@RequiredArgsConstructor
public class WMReportController extends BaseManagerController {

    private final ReportService reportService;
    private final ProductService productService;

    // UC27: Physical Inventory with Pagination
    @GetMapping("/inventory")
    public String viewPhysicalInventory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session, Model model) {

        Integer warehouseId = getManagerWarehouseId(session);
        Pageable pageable = PageRequest.of(page, size);
        Page<PhysicalInventoryDTO> inventoryPage = reportService.getPhysicalInventory(warehouseId, pageable);

        model.addAttribute("inventoryPage", inventoryPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("activePage", "report-inventory");
        return "warehouse/manager/report-inventory";
    }

    // UC28: Inbound Report with Pagination
    @GetMapping("/inbound")
    public String viewInboundReport(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session, Model model) {

        Integer warehouseId = getManagerWarehouseId(session);
        Pageable pageable = PageRequest.of(page, size);
        Page<InboundReportDTO> inboundPage = reportService.getInboundReport(warehouseId, pageable);

        model.addAttribute("inboundPage", inboundPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("activePage", "report-inbound");
        return "warehouse/manager/report-inbound";
    }

    // UC29: Outbound Report with Pagination
    @GetMapping("/outbound")
    public String viewOutboundReport(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session, Model model) {

        Integer warehouseId = getManagerWarehouseId(session);
        Pageable pageable = PageRequest.of(page, size);
        Page<OutboundReportDTO> outboundPage = reportService.getOutboundReport(warehouseId, pageable);

        model.addAttribute("outboundPage", outboundPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("activePage", "report-outbound");
        return "warehouse/manager/report-outbound";
    }

    // UC30: In-Out-Balance (Thẻ kho) - Không phân trang để giữ tính lũy kế
    @GetMapping("/in-out-balance")
    public String viewInOutBalanceReport(
            @RequestParam(required = false) Integer productId,
            HttpSession session, Model model) {

        Integer warehouseId = getManagerWarehouseId(session);
        model.addAttribute("products", productService.getAllProducts());

        if (productId != null) {
            model.addAttribute("balanceList", reportService.getInOutBalanceReport(warehouseId, productId));
            model.addAttribute("selectedProductId", productId);
        }
        model.addAttribute("activePage", "report-balance");
        return "warehouse/manager/report-balance";
    }
}