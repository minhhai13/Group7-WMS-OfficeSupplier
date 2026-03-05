package com.minhhai.wms.controller.admin;

import com.minhhai.wms.dto.ProductUoMConversionDTO;
import com.minhhai.wms.entity.Product;
import com.minhhai.wms.service.ProductService;
import com.minhhai.wms.service.ProductUoMConversionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/warehouse/uom-conversions")
@RequiredArgsConstructor
public class UoMConversionController {

    private final ProductUoMConversionService conversionService;
    private final ProductService productService;

    @GetMapping
    public String list(@RequestParam(name = "productId", required = false) Integer productId, Model model) {
        List<Product> products = productService.findAllActive();
        model.addAttribute("activePage", "warehouse-uom-conversions");
        model.addAttribute("products", products);

        if (productId != null) {
            model.addAttribute("selectedProductId", productId);
            model.addAttribute("conversions", conversionService.findByProductId(productId));
            productService.findById(productId).ifPresent(p -> {
                model.addAttribute("selectedProduct", p);
                model.addAttribute("baseUoM", p.getBaseUoM());
            });
        }
        return "warehouse/uom-conversion-list";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute ProductUoMConversionDTO conversionDTO,
                       RedirectAttributes redirectAttributes) {

        try {
            conversionService.save(conversionDTO);
            redirectAttributes.addFlashAttribute("success", "Conversion saved successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }

        return "redirect:/warehouse/uom-conversions?productId=" + conversionDTO.getProductId();
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable(name = "id") Integer id,
                         @RequestParam(name = "productId") Integer productId,
                         RedirectAttributes redirectAttributes) {
        conversionService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Conversion deleted.");
        return "redirect:/warehouse/uom-conversions?productId=" + productId;
    }
}
