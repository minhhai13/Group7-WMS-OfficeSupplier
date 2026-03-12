package com.minhhai.wms.controller.transfer;

import com.minhhai.wms.dto.ProductDTO;
import com.minhhai.wms.dto.TransferOrderDTO;
import com.minhhai.wms.entity.User;
import com.minhhai.wms.entity.Warehouse;
import com.minhhai.wms.service.ProductService;
import com.minhhai.wms.service.TransferOrderService;
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
public class TOController {
    private final TransferOrderService transferOrderService;
    private final ProductService productService;
    private final WarehouseService warehouseService;

    @GetMapping
    public String list(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "sourceWarehouseId", required = false) Integer sourceWarehouseId,
            Model model,
            HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");
        Integer warehouseId = user.getWarehouse().getWarehouseId();

        List<TransferOrderDTO> transferOrders =
                transferOrderService.getOutgoingTransferOrders(warehouseId, status, sourceWarehouseId);

        List<Warehouse> sourceWarehouses =
                warehouseService.findAllActiveExcluding(warehouseId);

        model.addAttribute("transfers", transferOrders);
        model.addAttribute("sourceWarehouse", sourceWarehouses);
        model.addAttribute("activePage", "transfer-requests");
        model.addAttribute("selectedSourceId", sourceWarehouseId);
        model.addAttribute("selectedStatus", status);

        return "transfer/to-list";
    }

    @GetMapping("/new")
    public String createForm(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        Integer warehouseId = user.getWarehouse().getWarehouseId();
        TransferOrderDTO dto = new TransferOrderDTO();

        dto.setDestinationWarehouseId(warehouseId);
        dto.setDestinationWarehouseName(user.getWarehouse().getWarehouseName());

        List<Warehouse> sourceWarehouses =
                warehouseService.findAllActiveExcluding(warehouseId);

        model.addAttribute("activePage", "transfer-requests");
        model.addAttribute("trDTO", dto);
        model.addAttribute("products", getActiveProductDTOs());
        model.addAttribute("sourceWarehouse", sourceWarehouses);
        return "transfer/to-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable(name = "id") Integer id,
                           Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            User user = (User) session.getAttribute("loggedInUser");
            Integer warehouseId = user.getWarehouse().getWarehouseId();

            TransferOrderDTO dto = transferOrderService.getTOById(id);
            model.addAttribute("activePage", "transfer-requests");
            model.addAttribute("trDTO", dto);
            model.addAttribute("products", getActiveProductDTOs());

            List<Warehouse> sourceWarehouses =
                warehouseService.findAllActiveExcluding(warehouseId);

            model.addAttribute("sourceWarehouse", sourceWarehouses);
            return "transfer/to-form";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/transfer/requests";
        }

    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("trDTO") TransferOrderDTO trDTO,
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

            return "transfer/to-form";
        }

        try {
            if ("submit".equals(action)) {
                transferOrderService.submitTO(trDTO, user);
                redirectAttributes.addFlashAttribute("success", "Transfer Order submitted successfully.");
            } else {
                transferOrderService.saveDraftTO(trDTO, user);
                redirectAttributes.addFlashAttribute("success", "Transfer Order saved as draft.");
            }

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute("error", e.getMessage());

            if (trDTO.getToId() != null) {
                return "redirect:/transfer/requests/" + trDTO.getToId() + "/edit";
            }

            return "redirect:/transfer/requests/new";
        }

        return "redirect:/transfer/requests";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable(name = "id") Integer id,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");

        if (user == null || user.getWarehouse() == null) {
            return "redirect:/login";
        }
        try {
            transferOrderService.deleteTO(id, user);
            redirectAttributes.addFlashAttribute("success", "Transfer Order deleted successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/transfer/requests";
    }

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