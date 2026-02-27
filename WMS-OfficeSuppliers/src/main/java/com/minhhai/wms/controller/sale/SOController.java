package com.minhhai.wms.controller.sale;

import com.minhhai.wms.dto.ProductDTO;
import com.minhhai.wms.dto.SaleOrderDTO;
import com.minhhai.wms.entity.Partner;
import com.minhhai.wms.entity.User;
import com.minhhai.wms.service.PartnerService;
import com.minhhai.wms.service.ProductService;
import com.minhhai.wms.service.SalesOrderService;
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
@RequestMapping("/sales/orders")
@RequiredArgsConstructor
public class SOController {

    private final SalesOrderService soService;
    private final PartnerService partnerService;
    private final ProductService productService;

    // ==================== List ====================

    @GetMapping
    public String list(@RequestParam(name = "status", required = false) String status,
                       @RequestParam(name = "customerId", required = false) Integer customerId,
                       Model model, HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");
        Integer warehouseId = user.getWarehouse().getWarehouseId();

        List<SaleOrderDTO> orders = soService.getSOsByWarehouse(warehouseId, status, customerId);
        List<Partner> customers = partnerService.findByType("Customer");

        model.addAttribute("activePage", "sales-orders");
        model.addAttribute("orders", orders);
        model.addAttribute("customers", customers);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedCustomerId", customerId);
        return "sales/so-list";
    }

    // ==================== Create Form ====================

    @GetMapping("/new")
    public String createForm(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");

        SaleOrderDTO dto = new SaleOrderDTO();
        dto.setWarehouseId(user.getWarehouse().getWarehouseId());
        dto.setWarehouseName(user.getWarehouse().getWarehouseName());

        model.addAttribute("activePage", "sales-orders");
        model.addAttribute("soDTO", dto);
        model.addAttribute("customers", getActiveCustomers());
        model.addAttribute("products", getActiveProductDTOs());

        return "sales/so-form";
    }

    // ==================== Edit Form ====================

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable(name = "id") Integer id,
                           Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            SaleOrderDTO dto = soService.getSOById(id);
            model.addAttribute("activePage", "sales-orders");
            model.addAttribute("soDTO", dto);
            model.addAttribute("customers", getActiveCustomers());
            model.addAttribute("products", getActiveProductDTOs());

            return "sales/so-form";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/sales/orders";
        }
    }

    // ==================== Save (Draft or Submit) ====================

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("soDTO") SaleOrderDTO soDTO,
                       BindingResult bindingResult,
                       @RequestParam(name = "action") String action,
                       Model model, HttpSession session,
                       RedirectAttributes redirectAttributes) {

        User user = (User) session.getAttribute("loggedInUser");

        if (bindingResult.hasErrors()) {
            if (user.getWarehouse() != null) {
                soDTO.setWarehouseName(user.getWarehouse().getWarehouseName());
            }
            model.addAttribute("activePage", "sales-orders");
            model.addAttribute("customers", getActiveCustomers());
            model.addAttribute("products", getActiveProductDTOs());
            model.addAttribute("error", "Please fix the errors below.");
            return "sales/so-form";
        }

        try {
            if ("submit".equals(action)) {
                soService.submitForApproval(soDTO, user);
                redirectAttributes.addFlashAttribute("success", "Sales Order submitted for approval successfully.");
            } else {
                soService.saveDraft(soDTO, user);
                redirectAttributes.addFlashAttribute("success", "Sales Order saved as draft successfully.");
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            if (soDTO.getSoId() != null) {
                return "redirect:/sales/orders/" + soDTO.getSoId() + "/edit";
            }
            return "redirect:/sales/orders/new";
        }

        return "redirect:/sales/orders";
    }

    // ==================== Delete ====================

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            soService.deleteSO(id);
            redirectAttributes.addFlashAttribute("success", "Sales Order deleted successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/sales/orders";
    }

    // ==================== Helpers ====================

    private List<Partner> getActiveCustomers() {
        return partnerService.findByType("Customer").stream()
                .filter(Partner::getIsActive)
                .toList();
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
