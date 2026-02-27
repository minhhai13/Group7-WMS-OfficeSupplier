package com.minhhai.wms.controller.admin;

import com.minhhai.wms.service.UserService;
import com.minhhai.wms.service.WarehouseService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final WarehouseService warehouseService;
    private final UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        model.addAttribute("activePage", "admin-dashboard");
        model.addAttribute("warehouseCount", warehouseService.findAll().size());
        model.addAttribute("userCount", userService.findAll().size());
        return "admin/dashboard";
    }
}
