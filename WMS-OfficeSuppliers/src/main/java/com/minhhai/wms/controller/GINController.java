package com.minhhai.wms.controller;

import com.minhhai.wms.dto.GoodsIssueDetailDTO;
import com.minhhai.wms.dto.GoodsIssueNoteDTO;
import com.minhhai.wms.entity.User;
import com.minhhai.wms.service.GoodsIssueNoteService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/storekeeper/gin")
@RequiredArgsConstructor
public class GINController {

    private final GoodsIssueNoteService ginService;

    // ==================== List ====================

    @GetMapping
    public String list(@RequestParam(name = "status", required = false, defaultValue = "Draft") String status,
                       Model model, HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");
        Integer warehouseId = user.getWarehouse().getWarehouseId();

        String filterStatus = "All".equals(status) ? null : status;
        List<GoodsIssueNoteDTO> gins = ginService.getGINsByWarehouse(warehouseId, filterStatus);

        model.addAttribute("activePage", "storekeeper-gin");
        model.addAttribute("gins", gins);
        model.addAttribute("selectedStatus", status);
        return "storekeeper/gin-list";
    }

    // ==================== Issue Form ====================

    @GetMapping("/{id}/issue")
    public String issueForm(@PathVariable(name = "id") Integer id,
                            Model model, RedirectAttributes redirectAttributes) {
        try {
            GoodsIssueNoteDTO dto = ginService.getGINById(id);
            model.addAttribute("activePage", "storekeeper-gin");
            model.addAttribute("ginDTO", dto);
            return "storekeeper/gin-form";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/storekeeper/gin";
        }
    }

    // ==================== Post ====================

    @PostMapping("/{id}/post")
    public String postGIN(@PathVariable(name = "id") Integer id,
                          @RequestParam(name = "giDetailId") List<Integer> giDetailIds,
                          @RequestParam(name = "issuedQty") List<Integer> issuedQtys,
                          RedirectAttributes redirectAttributes) {
        try {
            // Build detail DTOs from form params
            List<GoodsIssueDetailDTO> details = new java.util.ArrayList<>();
            for (int i = 0; i < giDetailIds.size(); i++) {
                GoodsIssueDetailDTO dto = new GoodsIssueDetailDTO();
                dto.setGiDetailId(giDetailIds.get(i));
                dto.setIssuedQty(issuedQtys.get(i));
                details.add(dto);
            }

            String message = ginService.postGIN(id, details);
            redirectAttributes.addFlashAttribute("success", message);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/storekeeper/gin";
    }
}
