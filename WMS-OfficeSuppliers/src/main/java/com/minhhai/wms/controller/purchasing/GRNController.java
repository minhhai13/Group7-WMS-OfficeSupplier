package com.minhhai.wms.controller.purchasing;

import com.minhhai.wms.dto.GoodsReceiptDetailDTO;
import com.minhhai.wms.dto.GoodsReceiptNoteDTO;
import com.minhhai.wms.entity.User;
import com.minhhai.wms.service.GoodsReceiptNoteService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/storekeeper/grn")
@RequiredArgsConstructor
public class GRNController {

    private final GoodsReceiptNoteService grnService;

    // ==================== List ====================

    @GetMapping
    public String list(@RequestParam(name = "status", required = false, defaultValue = "Draft") String status,
                       Model model, HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");
        Integer warehouseId = user.getWarehouse().getWarehouseId();

        String filterStatus = "All".equals(status) ? null : status;
        List<GoodsReceiptNoteDTO> grns = grnService.getGRNsByWarehouse(warehouseId, filterStatus);

        model.addAttribute("activePage", "storekeeper-grn");
        model.addAttribute("grns", grns);
        model.addAttribute("selectedStatus", status);
        return "storekeeper/grn-list";
    }

    // ==================== Receive ====================

    @GetMapping("/{id}/receive")
    public String receiveForm(@PathVariable(name = "id") Integer id,
                              Model model, RedirectAttributes redirectAttributes) {
        try {
            GoodsReceiptNoteDTO dto = grnService.getGRNById(id);
            model.addAttribute("activePage", "storekeeper-grn");
            model.addAttribute("grnDTO", dto);
            return "storekeeper/grn-form";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/storekeeper/grn";
        }
    }

    // ==================== Post ====================

    @PostMapping("/{id}/post")
    public String postGRN(@PathVariable(name = "id") Integer id,
                          @RequestParam(name = "grDetailId") List<Integer> grDetailIds,
                          @RequestParam(name = "receivedQty") List<Integer> receivedQtys,
                          RedirectAttributes redirectAttributes) {
        try {
            // Build detail DTOs from form params
            List<GoodsReceiptDetailDTO> details = new java.util.ArrayList<>();
            for (int i = 0; i < grDetailIds.size(); i++) {
                GoodsReceiptDetailDTO dto = new GoodsReceiptDetailDTO();
                dto.setGrDetailId(grDetailIds.get(i));
                dto.setReceivedQty(receivedQtys.get(i));
                details.add(dto);
            }

            String message = grnService.postGRN(id, details);
            redirectAttributes.addFlashAttribute("success", message);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/storekeeper/grn";
    }
}
