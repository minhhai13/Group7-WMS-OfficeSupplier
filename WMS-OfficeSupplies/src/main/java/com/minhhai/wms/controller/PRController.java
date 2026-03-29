package com.minhhai.wms.controller;

import com.minhhai.wms.dto.PurchaseRequestDTO;
import com.minhhai.wms.entity.Partner;
import com.minhhai.wms.entity.User;
import com.minhhai.wms.service.PartnerService;
import com.minhhai.wms.service.PurchaseRequestService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/purchasing/requests")
@RequiredArgsConstructor
public class PRController {

    private final PurchaseRequestService prService;
    private final PartnerService partnerService;

    // ==================== PR List ====================

    @GetMapping
    public String list(@RequestParam(name = "status", required = false, defaultValue = "All") String status,
                       Model model, HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");
        Integer warehouseId = user.getWarehouse().getWarehouseId();

        List<PurchaseRequestDTO> prs = prService.getPRsByWarehouse(warehouseId, status);

        model.addAttribute("activePage", "purchasing-requests");
        model.addAttribute("prs", prs);
        model.addAttribute("selectedStatus", status);
        return "purchasing/pr-list";
    }

    // ==================== Create PO from PR — Form ====================

    @GetMapping("/create-po")
    public String createPOForm(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        Integer warehouseId = user.getWarehouse().getWarehouseId();

        List<PurchaseRequestDTO> approvedPRs = prService.getApprovedPRsForConversion(warehouseId);
        List<Partner> suppliers = partnerService.findByType("Supplier").stream()
                .filter(Partner::getIsActive).toList();

        model.addAttribute("activePage", "purchasing-requests");
        model.addAttribute("approvedPRs", approvedPRs);
        model.addAttribute("suppliers", suppliers);
        return "purchasing/pr-create-po";
    }

    // ==================== Generate PO from selected PRs ====================

    @PostMapping("/generate-po")
    public String generatePO(@RequestParam(name = "prIds", required = false) List<Integer> prIds,
                             @RequestParam(name = "supplierId") Integer supplierId,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        try {
            User user = (User) session.getAttribute("loggedInUser");
            String poNumber = prService.convertPRsToPO(prIds, supplierId, user);
            redirectAttributes.addFlashAttribute("success",
                    "Purchase Order " + poNumber + " đã được tạo thành công từ các PR đã chọn. PO đang chờ duyệt.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/purchasing/requests/create-po";
        }
        return "redirect:/purchasing/requests";
    }
}
