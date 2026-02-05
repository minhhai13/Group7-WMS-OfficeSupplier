package com.minhhai.wms.controller;

import com.minhhai.wms.entity.Warehouse;
import com.minhhai.wms.service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("warehouses", warehouseService.findAll());
        return "warehouse/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("warehouse", Warehouse.builder().build());
        return "warehouse/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Warehouse> opt = warehouseService.findById(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy kho.");
            return "redirect:/warehouses";
        }
        model.addAttribute("warehouse", opt.get());
        return "warehouse/form";
    }

    @PostMapping("/save")
    public String save(
            @Valid @ModelAttribute("warehouse") Warehouse warehouse,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        Integer id = warehouse.getWarehouseId();
        String code = warehouse.getWarehouseCode();
        if (code != null && !code.isBlank()) {
            boolean duplicate = (id == null)
                    ? warehouseService.existsByWarehouseCode(code)
                    : warehouseService.existsByWarehouseCodeExcludingId(code, id);
            if (duplicate) {
                bindingResult.rejectValue("warehouseCode", "duplicate", "Mã kho đã tồn tại.");
            }
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("warehouse", warehouse);
            return "warehouse/form";
        }

        warehouseService.save(warehouse);
        redirectAttributes.addFlashAttribute("success", id == null ? "Thêm kho thành công." : "Cập nhật kho thành công.");
        return "redirect:/warehouses";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        if (warehouseService.findById(id).isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy kho.");
            return "redirect:/warehouses";
        }
        warehouseService.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Đã xóa kho.");
        return "redirect:/warehouses";
    }
}
