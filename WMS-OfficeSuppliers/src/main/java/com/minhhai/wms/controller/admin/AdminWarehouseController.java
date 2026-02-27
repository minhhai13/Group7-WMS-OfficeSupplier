package com.minhhai.wms.controller.admin;

import com.minhhai.wms.dto.WarehouseDTO;
import com.minhhai.wms.entity.Warehouse;
import com.minhhai.wms.service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/warehouses")
@RequiredArgsConstructor
public class AdminWarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    public String list(@RequestParam(name = "keyword", required = false) String keyword, Model model) {
        List<Warehouse> warehouses;
        if (keyword != null && !keyword.isBlank()) {
            warehouses = warehouseService.search(keyword);
            model.addAttribute("keyword", keyword);
        } else {
            warehouses = warehouseService.findAll();
        }
        model.addAttribute("activePage", "admin-warehouses");
        model.addAttribute("warehouses", warehouses);
        return "admin/warehouse-list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("activePage", "admin-warehouses");
        model.addAttribute("warehouse", new WarehouseDTO());
        return "admin/warehouse-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable(name = "id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        return warehouseService.findById(id)
                .map(warehouse -> {
                    WarehouseDTO dto = mapToDTO(warehouse);
                    model.addAttribute("activePage", "admin-warehouses");
                    model.addAttribute("warehouse", dto);
                    return "admin/warehouse-form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Warehouse not found.");
                    return "redirect:/admin/warehouses";
                });
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("warehouse") WarehouseDTO warehouseDTO,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("activePage", "admin-warehouses");
            return "admin/warehouse-form";
        }

        try {
            warehouseService.save(warehouseDTO);
            redirectAttributes.addFlashAttribute("success", "Warehouse saved successfully.");
        } catch (IllegalArgumentException e) {
            if (e.getMessage().toLowerCase().contains("code")) {
                bindingResult.rejectValue("warehouseCode", "error.warehouse", e.getMessage());
            } else {
                model.addAttribute("error", e.getMessage());
            }
            model.addAttribute("activePage", "admin-warehouses");
            return "admin/warehouse-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/admin/warehouses";
        }

        return "redirect:/admin/warehouses";
    }

    @PostMapping("/{id}/toggle")
    public String toggleActive(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes) {
        warehouseService.toggleActive(id);
        redirectAttributes.addFlashAttribute("success", "Warehouse status updated.");
        return "redirect:/admin/warehouses";
    }

    private WarehouseDTO mapToDTO(Warehouse warehouse) {
        return WarehouseDTO.builder()
                .warehouseId(warehouse.getWarehouseId())
                .warehouseCode(warehouse.getWarehouseCode())
                .warehouseName(warehouse.getWarehouseName())
                .address(warehouse.getAddress())
                .isActive(warehouse.getIsActive())
                .build();
    }


}
