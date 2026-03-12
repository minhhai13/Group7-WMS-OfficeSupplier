package com.minhhai.wms.controller.purchasing;

import com.minhhai.wms.dto.ProductDTO;
import com.minhhai.wms.dto.PurchaseOrderDTO;
import com.minhhai.wms.entity.Partner;
import com.minhhai.wms.entity.User;
import com.minhhai.wms.service.PartnerService;
import com.minhhai.wms.service.ProductService;
import com.minhhai.wms.service.PurchaseOrderService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/purchasing/orders")
@RequiredArgsConstructor
public class POController {

    private final PurchaseOrderService poService;
    private final PartnerService partnerService;
    private final ProductService productService;


    @GetMapping
    public String list(@RequestParam(name = "status", required = false) String status,
                       @RequestParam(name = "supplierId", required = false) Integer supplierId,
                       @RequestParam(name = "page", defaultValue = "1") int page,
                       @RequestParam(name = "size", defaultValue = "10") int size,
                       Model model, HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");
        Integer warehouseId = user.getWarehouse().getWarehouseId();

        // Spring Data JPA page index bắt đầu từ 0
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "poId"));
        Page<PurchaseOrderDTO> orderPage = poService.getPOsByWarehouse(warehouseId, status, supplierId, pageable);
        List<Partner> suppliers = partnerService.findByType("Supplier");

        model.addAttribute("activePage", "purchasing-orders");
        model.addAttribute("orderPage", orderPage); // Truyền nguyên object Page sang View
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedSupplierId", supplierId);
        return "purchasing/po-list";
    }

    // ==================== Create Form ====================

    @GetMapping("/new")
    public String createForm(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");

        PurchaseOrderDTO dto = new PurchaseOrderDTO();
        dto.setWarehouseId(user.getWarehouse().getWarehouseId());
        dto.setWarehouseName(user.getWarehouse().getWarehouseName());

        model.addAttribute("activePage", "purchasing-orders");
        model.addAttribute("poDTO", dto);
        model.addAttribute("suppliers", getActiveSuppliers());
        model.addAttribute("products", getActiveProductDTOs());

        return "purchasing/po-form";
    }

    // ==================== Edit Form ====================

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable(name = "id") Integer id,
                           Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            PurchaseOrderDTO dto = poService.getPOById(id);
            model.addAttribute("activePage", "purchasing-orders");
            model.addAttribute("poDTO", dto);
            model.addAttribute("suppliers", getActiveSuppliers());
            model.addAttribute("products", getActiveProductDTOs());

            return "purchasing/po-form";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/purchasing/orders";
        }
    }

    // ==================== Save (Draft or Submit) ====================

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("poDTO") PurchaseOrderDTO poDTO,
                       BindingResult bindingResult,
                       @RequestParam(name = "action") String action,
                       Model model, HttpSession session,
                       RedirectAttributes redirectAttributes) {

        User user = (User) session.getAttribute("loggedInUser");

        if (bindingResult.hasErrors()) {
            if (user.getWarehouse() != null) {
                poDTO.setWarehouseName(user.getWarehouse().getWarehouseName());
            }
            model.addAttribute("activePage", "purchasing-orders");
            model.addAttribute("suppliers", getActiveSuppliers());
            model.addAttribute("products", getActiveProductDTOs());
            model.addAttribute("error", "Please fix the errors below.");
            return "purchasing/po-form";
        }

        try {
            if ("submit".equals(action)) {
                poService.submitForApproval(poDTO, user);
                redirectAttributes.addFlashAttribute("success", "Purchase Order submitted for approval successfully.");
            } else {
                poService.saveDraft(poDTO, user);
                redirectAttributes.addFlashAttribute("success", "Purchase Order saved as draft successfully.");
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            // If it's a new PO (no ID), redirect to new form; otherwise back to edit
            if (poDTO.getPoId() != null) {
                return "redirect:/purchasing/orders/" + poDTO.getPoId() + "/edit";
            }
            return "redirect:/purchasing/orders/new";
        }

        return "redirect:/purchasing/orders";
    }

    // ==================== Delete ====================

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            poService.deletePO(id);
            redirectAttributes.addFlashAttribute("success", "Purchase Order deleted successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/purchasing/orders";
    }

    // ==================== Helpers ====================

    private List<Partner> getActiveSuppliers() {
        return partnerService.findByType("Supplier").stream()
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
