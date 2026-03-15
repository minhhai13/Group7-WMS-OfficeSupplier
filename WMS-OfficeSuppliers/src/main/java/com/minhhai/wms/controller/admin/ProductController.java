package com.minhhai.wms.controller.admin;

import com.minhhai.wms.dto.ProductDTO;
import com.minhhai.wms.entity.Product;
import com.minhhai.wms.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/warehouse/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    private static final int PAGE_SIZE = 10;

    @GetMapping
    public String list(@RequestParam(name = "keyword", required = false) String keyword,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       Model model) {
        if (page < 0) page = 0;

        Page<Product> productPage = productService.findPaginated(keyword, page, PAGE_SIZE);

        if (page > 0 && page >= productPage.getTotalPages()) {
            page = Math.max(0, productPage.getTotalPages() - 1);
            productPage = productService.findPaginated(keyword, page, PAGE_SIZE);
        }

        model.addAttribute("activePage", "warehouse-products");
        model.addAttribute("productPage", productPage);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalElements", productPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        return "warehouse/product-list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("activePage", "warehouse-products");
        model.addAttribute("product", new ProductDTO());
        return "warehouse/product-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable(name = "id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        return productService.findById(id)
                .map(product -> {
                    ProductDTO dto = mapToDTO(product);
                    model.addAttribute("activePage", "warehouse-products");
                    model.addAttribute("product", dto);
                    return "warehouse/product-form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Product not found.");
                    return "redirect:/warehouse/products";
                });
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("product") ProductDTO productDTO,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("activePage", "warehouse-products");
            return "warehouse/product-form";
        }

        try {
            productService.save(productDTO);
            redirectAttributes.addFlashAttribute("success", "Product saved successfully.");
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage().toLowerCase();
            if (msg.contains("sku") || msg.contains("exists")) {
                bindingResult.rejectValue("sku", "error.product", e.getMessage());
            } else {
                model.addAttribute("error", e.getMessage());
            }
            model.addAttribute("activePage", "warehouse-products");
            return "warehouse/product-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/warehouse/products";
        }

        return "redirect:/warehouse/products";
    }

    @PostMapping("/{id}/toggle")
    public String toggleActive(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes) {
        productService.toggleActive(id);
        redirectAttributes.addFlashAttribute("success", "Product status updated.");
        return "redirect:/warehouse/products";
    }

    private ProductDTO mapToDTO(Product product) {
        return ProductDTO.builder()
                .productId(product.getProductId())
                .sku(product.getSku())
                .productName(product.getProductName())
                .unitWeight(product.getUnitWeight())
                .baseUoM(product.getBaseUoM())
                .minStockLevel(product.getMinStockLevel())
                .isActive(product.getIsActive())
                .build();
    }
}
