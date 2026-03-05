package com.minhhai.wms.controller.transfer;

import com.minhhai.wms.dto.TransferRequestDTO;
import com.minhhai.wms.entity.User;
import com.minhhai.wms.entity.Warehouse;
import com.minhhai.wms.service.TransferRequestService;
import com.minhhai.wms.service.WarehouseService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/transfer/approvals")
@RequiredArgsConstructor
public class TRApprovalController {
    private final TransferRequestService transferRequestService;
    private final WarehouseService warehouseService;

    @GetMapping
    public String list(@RequestParam(name = "status", required = false, defaultValue = "All") String status,
                       @RequestParam(name = "destWarehouseId", required = false) Integer destWarehouseId,
                       Model model, HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        Integer warehouseId = user.getWarehouse().getWarehouseId();
        String filterStatus = "All".equals(status) ? null : status;

        List<TransferRequestDTO> transfers =
                transferRequestService.getIncomingWarehouseTransferRequests(warehouseId, filterStatus, destWarehouseId);

        List<Warehouse> sourceWarehouses =
                warehouseService.findAllActiveExcluding(warehouseId);

        model.addAttribute("activePage", "transfer-approvals");
        model.addAttribute("transfers", transfers);
        model.addAttribute("sourceWarehouses", sourceWarehouses);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedDestId", destWarehouseId);

        return "transfer/tr-approved-list";
    }

    @GetMapping("/{id}/review")
    public String review(@PathVariable Integer id,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        try {
            TransferRequestDTO dto = transferRequestService.getTRById(id);

            model.addAttribute("activePage", "transfer-approvals");
            model.addAttribute("trDTO", dto);

            return "transfer/tr-approved-review";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/transfer/approvals";
        }
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Integer id,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {

        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            transferRequestService.approveTR(id, user);
            redirectAttributes.addFlashAttribute("success",
                    "Transfer Request has been approved.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/transfer/approvals";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Integer id,
                         @RequestParam("reason") String reason,
                         RedirectAttributes redirectAttributes) {

        if (reason == null || reason.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Reject reason is required.");
            return "redirect:/transfer/approvals/" + id + "/review";
        }

        try {
            transferRequestService.rejectTR(id, reason.trim());
            redirectAttributes.addFlashAttribute("success",
                    "Transfer Request has been rejected.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/transfer/approvals";
    }
}
