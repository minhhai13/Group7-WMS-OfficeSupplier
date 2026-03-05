package com.minhhai.wms.controller.sale;

import com.minhhai.wms.dto.SaleOrderDTO;
import com.minhhai.wms.entity.Partner;
import com.minhhai.wms.entity.User;
import com.minhhai.wms.service.PartnerService;
import com.minhhai.wms.service.SalesOrderService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/sales/approvals")
@RequiredArgsConstructor
public class SOApprovalController {

    private final SalesOrderService soService;
    private final PartnerService partnerService;

    // ==================== List ====================

    @GetMapping
    public String list(@RequestParam(name = "status", required = false, defaultValue = "Pending Approval") String status,
                       @RequestParam(name = "customerId", required = false) Integer customerId,
                       Model model, HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");
        Integer warehouseId = user.getWarehouse().getWarehouseId();

        String filterStatus = "All".equals(status) ? null : status;
        List<SaleOrderDTO> orders = soService.getSOsByWarehouse(warehouseId, filterStatus, customerId);
        List<Partner> customers = partnerService.findByType("Customer");

        model.addAttribute("activePage", "sales-approvals");
        model.addAttribute("orders", orders);
        model.addAttribute("customers", customers);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedCustomerId", customerId);
        return "sales/approval-list";
    }

    // ==================== Review ====================

    @GetMapping("/{id}/review")
    public String review(@PathVariable(name = "id") Integer id,
                         Model model, RedirectAttributes redirectAttributes) {
        try {
            SaleOrderDTO dto = soService.getSOById(id);
            model.addAttribute("activePage", "sales-approvals");
            model.addAttribute("soDTO", dto);
            return "sales/approval-review";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/sales/approvals";
        }
    }

    // ==================== Approve ====================

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable(name = "id") Integer id,
                          HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        try {
            String result = soService.approveSO(id, user);
            if (result == null) {
                redirectAttributes.addFlashAttribute("success", "SO đã được xử lý.");
            } else if (result.contains("Waiting") || result.contains("chờ hàng")) {
                // Branch A: SO → Waiting for Stock (PR not yet completed)
                redirectAttributes.addFlashAttribute("success", result);
            } else {
                // Branch B: GIN created
                redirectAttributes.addFlashAttribute("success",
                        "SO đã được duyệt. Phiếu xuất kho " + result + " đã được tạo tự động.");
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/sales/approvals/" + id + "/review";
        }
        return "redirect:/sales/approvals";
    }

    // ==================== Reject ====================

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable(name = "id") Integer id,
                         @RequestParam(name = "reason") String reason,
                         RedirectAttributes redirectAttributes) {
        try {
            soService.rejectSO(id, reason);
            redirectAttributes.addFlashAttribute("success", "SO has been rejected.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/sales/approvals";
    }
}
