package com.minhhai.wms.controller.transfer;

import com.minhhai.wms.dto.ProductDTO;
import com.minhhai.wms.dto.TransferRequestDTO;
import com.minhhai.wms.entity.User;
import com.minhhai.wms.entity.Warehouse;
import com.minhhai.wms.service.ProductService;
import com.minhhai.wms.service.TransferRequestService;
import com.minhhai.wms.service.WarehouseService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/transfer/requests")
@RequiredArgsConstructor
public class TRController {
    private final TransferRequestService transferRequestService;
    private final ProductService productService;
    private final WarehouseService warehouseService;

    // ==================== List ====================
    @GetMapping
    public String list(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "sourceWarehouseId", required = false) Integer sourceWarehouseId,
            Model model,
            HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");
        Integer warehouseId = user.getWarehouse().getWarehouseId();

        List<TransferRequestDTO> transferRequests =
                transferRequestService.getOutgoingTransferRequests(warehouseId, status, sourceWarehouseId);

        List<Warehouse> sourceWarehouses =
                warehouseService.findAllActiveExcluding(warehouseId);

        model.addAttribute("transfers", transferRequests);
        model.addAttribute("sourceWarehouse", sourceWarehouses);
        model.addAttribute("activePage", "transfer-requests");
        model.addAttribute("selectedSourceId", sourceWarehouseId);
        model.addAttribute("selectedStatus", status);

        return "transfer/tr-list";
    }

    // ==================== Create Form ====================
    @GetMapping("/new")
    public String createForm(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        Integer warehouseId = user.getWarehouse().getWarehouseId();
        TransferRequestDTO dto = new TransferRequestDTO();

        dto.setDestinationWarehouseId(warehouseId);
        dto.setDestinationWarehouseName(user.getWarehouse().getWarehouseName());

        List<Warehouse> sourceWarehouses =
                warehouseService.findAllActiveExcluding(warehouseId);

        model.addAttribute("activePage", "transfer-requests");
        model.addAttribute("trDTO", dto);
        model.addAttribute("products", getActiveProductDTOs());
        model.addAttribute("sourceWarehouse", sourceWarehouses);
        return "transfer/tr-form";
    }
    // ==================== Edit Form ====================

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable(name = "id") Integer id,
                           Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            User user = (User) session.getAttribute("loggedInUser");
            Integer warehouseId = user.getWarehouse().getWarehouseId();

            TransferRequestDTO dto = transferRequestService.getTRById(id);
            model.addAttribute("activePage", "transfer-requests");
            model.addAttribute("trDTO", dto);
            model.addAttribute("products", getActiveProductDTOs());

            List<Warehouse> sourceWarehouses =
                    warehouseService.findAllActiveExcluding(warehouseId);

            model.addAttribute("sourceWarehouse", sourceWarehouses);
            return "transfer/tr-form";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/transfer/requests";
        }

    }

    // ==================== Save (Draft or Submit) ====================

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("trDTO") TransferRequestDTO trDTO,
                       BindingResult bindingResult,
                       @RequestParam("action") String action,
                       Model model,
                       HttpSession session,
                       RedirectAttributes redirectAttributes) {

        User user = (User) session.getAttribute("loggedInUser");

        if (user == null || user.getWarehouse() == null) {
            return "redirect:/login";
        }

        Integer warehouseId = user.getWarehouse().getWarehouseId();

        if (bindingResult.hasErrors()) {

            trDTO.setDestinationWarehouseId(warehouseId);
            trDTO.setDestinationWarehouseName(user.getWarehouse().getWarehouseName());

            List<Warehouse> sourceWarehouses =
                    warehouseService.findAllActiveExcluding(warehouseId);

            model.addAttribute("activePage", "transfer-requests");
            model.addAttribute("sourceWarehouse", sourceWarehouses);
            model.addAttribute("products", getActiveProductDTOs());
            model.addAttribute("error", "Please fix the errors below.");

            return "transfer/tr-form";
        }

        try {
            if ("submit".equals(action)) {
                transferRequestService.submitTR(trDTO, user);
                redirectAttributes.addFlashAttribute("success", "Transfer Request submitted successfully.");
            } else {
                transferRequestService.saveDraftTR(trDTO, user);
                redirectAttributes.addFlashAttribute("success", "Transfer Request saved as draft.");
            }

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute("error", e.getMessage());

            if (trDTO.getTrId() != null) {
                return "redirect:/transfer/requests/" + trDTO.getTrId() + "/edit";
            }

            return "redirect:/transfer/requests/new";
        }

        return "redirect:/transfer/requests";
    }

    // ==================== Delete ====================

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable(name = "id") Integer id,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");

        if (user == null || user.getWarehouse() == null) {
            return "redirect:/login";
        }
        try {
            transferRequestService.deleteTR(id, user);
            redirectAttributes.addFlashAttribute("success", "Transfer Request deleted successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/transfer/requests";
    }

    // ==================== Helpers ====================


    private List<ProductDTO> getActiveProductDTOs() {
        return productService.findAllActive().stream()
                .map(p -> ProductDTO.builder()
                        .productId(p.getProductId())
                        .sku(p.getSku())
                        .productName(p.getProductName())
                        .build())
                .toList();
    }
}
