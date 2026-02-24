package com.minhhai.wms.controller;

import com.minhhai.wms.entity.User;
import com.minhhai.wms.service.BinService;
import com.minhhai.wms.service.ProductService;
import com.minhhai.wms.service.PartnerService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/warehouse")
@RequiredArgsConstructor
public class WarehouseDashboardController {

    private final BinService binService;
    private final ProductService productService;
    private final PartnerService partnerService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        model.addAttribute("activePage", "warehouse-dashboard");

        Integer warehouseId = loggedInUser.getWarehouse() != null
                ? loggedInUser.getWarehouse().getWarehouseId() : null;

        model.addAttribute("binCount", warehouseId != null
                ? binService.findByWarehouseId(warehouseId).size() : 0);
        model.addAttribute("productCount", productService.findAll().size());
        model.addAttribute("partnerCount", partnerService.findAll().size());
        model.addAttribute("warehouseName", loggedInUser.getWarehouse() != null
                ? loggedInUser.getWarehouse().getWarehouseName() : "N/A");
        return "warehouse/dashboard";
    }
}
