package com.minhhai.wms.controller.transfer;

import com.minhhai.wms.dto.TransferOrderDTO;
import com.minhhai.wms.entity.User;
import com.minhhai.wms.entity.Warehouse;
import com.minhhai.wms.service.TransferOrderService;
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
public class TOApprovalController {
    private final TransferOrderService transferOrderService;
    private final WarehouseService warehouseService;

    @GetMapping
    public String list(Model model,
                       HttpSession session,
                       @RequestParam(name = "status", required = false) String status,
                       @RequestParam(name = "destWarehouseId", required = false) Integer destWarehouseId){
        User user = (User)session.getAttribute("loggedInUser");
        if(user == null || user.getWarehouse() == null) return "redirect:/login";

        Integer warehouseId = user.getWarehouse().getWarehouseId();

        String filterStatus = "All".equals(status) ? null : status;

        List<TransferOrderDTO> transferOrders = transferOrderService.getIncomingWHTransferOrders(destWarehouseId, warehouseId, filterStatus);
        List<Warehouse> destWarehouses = warehouseService.findAllActiveExcluding(warehouseId);

        model.addAttribute("transfers", transferOrders);
        model.addAttribute("destWarehouses", destWarehouses);
        model.addAttribute("activePage", "transfer-approvals");
        model.addAttribute("selectedDestId", destWarehouseId);
        model.addAttribute("selectedStatus", status);
        return "transfer/to-approved-list";
    }
    @GetMapping("/{id}/review")
    public String review(Model model,
                         HttpSession session,
                         @PathVariable Integer id,
                         RedirectAttributes ra){
        User user = (User)session.getAttribute("loggedInUser");
        if(user == null || user.getWarehouse() == null) return "redirect:/login";

        try{
            TransferOrderDTO dto = transferOrderService.getTRById(id);
            model.addAttribute("trDTO", dto);
            model.addAttribute("activePage", "transfer-approvals");
            return "transfer/to-approved-review";
        }catch(IllegalArgumentException e){
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/transfer/approvals";
        }

    }

    @PostMapping("/{id}/approve")
    public String approve(HttpSession session,
                          @PathVariable Integer id,
                          RedirectAttributes ra){
        User user = (User)session.getAttribute("loggedInUser");
        if(user == null || user.getWarehouse() == null) return "redirect:/login";

        try{
            transferOrderService.approveTR(id, user);
            ra.addFlashAttribute("success",
                    "Transfer Order approved. A Goods Issue Note has been auto-created for the source warehouse storekeeper.");
        }catch(IllegalArgumentException e){
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/transfer/approvals";

    }

    @PostMapping("/{id}/reject")
    public String reject(HttpSession session,
                         @PathVariable Integer id,
                         RedirectAttributes ra,
                         @RequestParam("reason") String reason){
        User user = (User)session.getAttribute("loggedInUser");
        if(user == null || user.getWarehouse() == null) return "redirect:/login";

        if (reason == null || reason.trim().isEmpty()) {
            ra.addFlashAttribute("error", "Reject reason is required.");
            return "redirect:/transfer/approvals/" + id + "/review";
        }
        try{
            transferOrderService.rejectTR(id, user, reason.trim());
            ra.addFlashAttribute("success", "Transfer Order rejected successfully.");
        }
        catch(IllegalArgumentException e){
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/transfer/approvals";
    }

}