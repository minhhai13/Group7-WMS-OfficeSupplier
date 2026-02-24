package com.minhhai.wms.controller;

import com.minhhai.wms.entity.PurchaseOrder;
import com.minhhai.wms.entity.User;
import com.minhhai.wms.service.PurchaseOrderService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/purchasing")
@RequiredArgsConstructor
public class POApprovalController {

    private final PurchaseOrderService poService;

    @GetMapping("/approval-dashboard")
    public String showApprovalDashboard(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user.getWarehouse() == null) {
            return "redirect:/403";
        }
        
        // Fetch only POs that are Pending Approval
        List<PurchaseOrder> pendingPOs = poService.getPOsByWarehouse(user.getWarehouse().getWarehouseId(), "Pending Approval");
        model.addAttribute("pendingPOs", pendingPOs);
        model.addAttribute("activePage", "approval-dashboard");
        return "purchasing/approval-dashboard";
    }

    @PostMapping("/approve/{id}")
    public String approvePO(@PathVariable("id") Integer poId,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        try {
            poService.approvePO(poId, user.getWarehouse().getWarehouseId());
            redirectAttributes.addFlashAttribute("successMessage", "Purchase Order #" + poId + " approved successfully! A Draft GRN has been generated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/purchasing/approval-dashboard";
    }

    @PostMapping("/reject/{id}")
    public String rejectPO(@PathVariable("id") Integer poId,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        try {
            poService.rejectPO(poId, user.getWarehouse().getWarehouseId());
            redirectAttributes.addFlashAttribute("successMessage", "Purchase Order #" + poId + " has been rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/purchasing/approval-dashboard";
    }
}
