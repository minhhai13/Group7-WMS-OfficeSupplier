package com.minhhai.wms.controller.purchasing;

import com.minhhai.wms.dto.PurchaseOrderDTO;
import com.minhhai.wms.entity.Partner;
import com.minhhai.wms.entity.User;
import com.minhhai.wms.service.PartnerService;
import com.minhhai.wms.service.PurchaseOrderService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/purchasing/approvals")
@RequiredArgsConstructor
public class POApprovalController {

    private final PurchaseOrderService poService;
    private final PartnerService partnerService;


    @GetMapping
    public String list(@RequestParam(name = "status", required = false, defaultValue = "Pending Approval") String status,
                       @RequestParam(name = "supplierId", required = false) Integer supplierId,
                       @RequestParam(name = "page", defaultValue = "1") int page,
                       @RequestParam(name = "size", defaultValue = "10") int size,
                       Model model, HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");
        Integer warehouseId = user.getWarehouse().getWarehouseId();

        String filterStatus = "All".equals(status) ? null : status;
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "poId"));
        Page<PurchaseOrderDTO> orderPage = poService.getPOsByWarehouse(warehouseId, filterStatus, supplierId, pageable);

        List<Partner> suppliers = partnerService.findByType("Supplier");

        model.addAttribute("activePage", "purchasing-approvals");
        model.addAttribute("orderPage", orderPage);
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedSupplierId", supplierId);
        return "purchasing/approval-list";
    }

    // ==================== Review ====================

    @GetMapping("/{id}/review")
    public String review(@PathVariable(name = "id") Integer id,
                         Model model, RedirectAttributes redirectAttributes) {
        try {
            PurchaseOrderDTO dto = poService.getPOById(id);
            model.addAttribute("activePage", "purchasing-approvals");
            model.addAttribute("poDTO", dto);
            return "purchasing/approval-review";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/purchasing/approvals";
        }
    }

    // ==================== Approve ====================

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable(name = "id") Integer id,
                          HttpSession session, RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        try {
            String grnNumber = poService.approvePO(id, user);
            redirectAttributes.addFlashAttribute("success",
                    "PO has been approved and GRN " + grnNumber + " has been generated.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/purchasing/approvals";
    }

    // ==================== Reject ====================

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable(name = "id") Integer id,
                         @RequestParam(name = "reason") String reason,
                         RedirectAttributes redirectAttributes) {
        try {
            poService.rejectPO(id, reason);
            redirectAttributes.addFlashAttribute("success", "PO has been rejected.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/purchasing/approvals";
    }
}
