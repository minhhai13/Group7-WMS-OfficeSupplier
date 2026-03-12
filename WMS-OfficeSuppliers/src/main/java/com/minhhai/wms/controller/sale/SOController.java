package com.minhhai.wms.controller.sale;

import com.minhhai.wms.dto.ProductDTO;
import com.minhhai.wms.dto.SaleOrderDTO;
import com.minhhai.wms.dto.StockCheckResult;
import com.minhhai.wms.entity.Partner;
import com.minhhai.wms.entity.User;
import com.minhhai.wms.service.PartnerService;
import com.minhhai.wms.service.ProductService;
import com.minhhai.wms.service.SalesOrderService;
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
@RequestMapping("/sales/orders")
@RequiredArgsConstructor
public class SOController {

    private final SalesOrderService soService;
    private final PartnerService partnerService;
    private final ProductService productService;


    @GetMapping
    public String list(@RequestParam(name = "status", required = false) String status,
                       @RequestParam(name = "customerId", required = false) Integer customerId,
                       @RequestParam(name = "page", defaultValue = "1") int page,
                       @RequestParam(name = "size", defaultValue = "10") int size,
                       Model model, HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");
        Integer warehouseId = user.getWarehouse().getWarehouseId();

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "soId"));
        Page<SaleOrderDTO> orderPage = soService.getSOsByWarehouse(warehouseId, status, customerId, pageable);
        List<Partner> customers = partnerService.findByType("Customer");

        model.addAttribute("activePage", "sales-orders");
        model.addAttribute("orderPage", orderPage);
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

    // ==================== Save (Draft / Submit / SubmitWithPR) ====================

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
            if ("draft".equals(action)) {
                // Save as Draft
                soService.saveDraft(soDTO, user);
                redirectAttributes.addFlashAttribute("success", "Sales Order saved as draft successfully.");
                return "redirect:/sales/orders";

            } else if ("submit".equals(action)) {
                // Step 1: Check ATP, do NOT create PR yet
                StockCheckResult result = soService.checkAndSubmit(soDTO, user, false);
                if (result.isHasShortage()) {
                    // Show shortage warning on the form — redirect back with shortage info
                    redirectAttributes.addFlashAttribute("shortages", result.getShortages());
                    redirectAttributes.addFlashAttribute("soIdForPR", result.getSoId());
                    redirectAttributes.addFlashAttribute("warning",
                            "Kho không đủ hàng cho một số sản phẩm. Bạn có muốn tạo Purchase Request (PR) kèm theo không?");
                    return "redirect:/sales/orders/" + result.getSoId() + "/edit";
                }
                // No shortage, submitted successfully
                redirectAttributes.addFlashAttribute("success", "Sales Order submitted for approval.");
                return "redirect:/sales/orders";

            } else if ("submitWithPR".equals(action)) {
                // Step 2: Submit WITH PR creation
                StockCheckResult result = soService.checkAndSubmit(soDTO, user, true);
                if (result.getPrNumber() != null) {
                    redirectAttributes.addFlashAttribute("success",
                            "SO đã gửi duyệt. Purchase Request " + result.getPrNumber() + " đã được tạo tự động cho lượng thiếu.");
                } else {
                    redirectAttributes.addFlashAttribute("success", "Sales Order submitted for approval.");
                }
                return "redirect:/sales/orders";

            } else {
                // Unknown action → save as draft
                soService.saveDraft(soDTO, user);
                redirectAttributes.addFlashAttribute("success", "Sales Order saved as draft.");
                return "redirect:/sales/orders";
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            if (soDTO.getSoId() != null) {
                return "redirect:/sales/orders/" + soDTO.getSoId() + "/edit";
            }
            return "redirect:/sales/orders/new";
        }
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
