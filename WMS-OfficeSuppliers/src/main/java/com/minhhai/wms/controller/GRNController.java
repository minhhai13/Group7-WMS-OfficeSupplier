package com.minhhai.wms.controller;

import com.minhhai.wms.dto.GoodsReceiptDetailDTO;
import com.minhhai.wms.dto.GoodsReceiptNoteDTO;
import com.minhhai.wms.entity.GoodsReceiptNote;
import com.minhhai.wms.entity.PurchaseOrder;
import com.minhhai.wms.entity.PurchaseOrderDetail;
import com.minhhai.wms.entity.User;
import com.minhhai.wms.service.GoodsReceiptNoteService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/storekeeper")
@RequiredArgsConstructor
public class GRNController {

    private final GoodsReceiptNoteService grnService;

    @GetMapping("/grn-list")
    public String listPendingGRNs(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user.getWarehouse() == null) {
            return "redirect:/403";
        }
        
        // Show pending GRNs (Status = Draft)
        List<GoodsReceiptNote> pendingGrns = grnService.getGRNsByWarehouse(user.getWarehouse().getWarehouseId(), "Draft");
        model.addAttribute("pendingGrns", pendingGrns);
        model.addAttribute("activePage", "grn-list");
        return "storekeeper/grn-list";
    }

    @GetMapping("/grn-process/{id}")
    public String showGRNProcessForm(@PathVariable("id") Integer grnId,
                                     Model model, HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        try {
            GoodsReceiptNote grn = grnService.getGRNById(grnId);
            if (!grn.getWarehouse().getWarehouseId().equals(user.getWarehouse().getWarehouseId())) {
                return "redirect:/403";
            }
            if (!"Draft".equals(grn.getGrStatus())) {
                redirectAttributes.addFlashAttribute("errorMessage", "This GRN is already posted.");
                return "redirect:/storekeeper/grn-list";
            }

            PurchaseOrder po = grn.getPurchaseOrder();

            GoodsReceiptNoteDTO dto = new GoodsReceiptNoteDTO();
            dto.setGrnId(grn.getGrnId());
            dto.setGrnNumber(grn.getGrnNumber());
            dto.setPoId(po.getPoId());
            dto.setPoNumber(po.getPoNumber());
            dto.setWarehouseId(grn.getWarehouse().getWarehouseId());
            dto.setGrStatus(grn.getGrStatus());

            List<GoodsReceiptDetailDTO> details = new ArrayList<>();
            for (PurchaseOrderDetail poDetail : po.getDetails()) {
                int remainingToReceive = poDetail.getOrderedQty() - poDetail.getReceivedQty();
                if (remainingToReceive > 0) {
                    GoodsReceiptDetailDTO detailDTO = new GoodsReceiptDetailDTO();
                    detailDTO.setPoDetailId(poDetail.getPoDetailId());
                    detailDTO.setProductId(poDetail.getProduct().getProductId());
                    detailDTO.setProductSku(poDetail.getProduct().getSku());
                    detailDTO.setProductName(poDetail.getProduct().getProductName());
                    detailDTO.setOrderedQty(poDetail.getOrderedQty());
                    detailDTO.setPreviousReceivedQty(poDetail.getReceivedQty());
                    detailDTO.setUom(poDetail.getUom());
                    // we don't set receivedQty or binId, user must fill
                    details.add(detailDTO);
                }
            }
            dto.setDetails(details);

            model.addAttribute("grnDTO", dto);
            model.addAttribute("activePage", "grn-process");
            model.addAttribute("warehouseId", user.getWarehouse().getWarehouseId());
            // passing unit weights into a hidden map so JS can use it to calculate weight
            model.addAttribute("poDetailsFull", po.getDetails());
            return "storekeeper/grn-process";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/storekeeper/grn-list";
        }
    }

    @PostMapping("/grn-process")
    public String processGRN(@Valid @ModelAttribute("grnDTO") GoodsReceiptNoteDTO grnDTO,
                             BindingResult bindingResult,
                             Model model, HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if (bindingResult.hasErrors()) {
            model.addAttribute("activePage", "grn-process");
            model.addAttribute("warehouseId", user.getWarehouse().getWarehouseId());
            // Need to reload full details to get weights for Javascript
            GoodsReceiptNote grn = grnService.getGRNById(grnDTO.getGrnId());
            model.addAttribute("poDetailsFull", grn.getPurchaseOrder().getDetails());
            return "storekeeper/grn-process";
        }

        try {
            grnService.processGRN(grnDTO, user.getWarehouse().getWarehouseId());
            redirectAttributes.addFlashAttribute("successMessage", "Goods Receipt Note processed successfully.");
            return "redirect:/storekeeper/grn-list";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/storekeeper/grn-list";
        }
    }
}
