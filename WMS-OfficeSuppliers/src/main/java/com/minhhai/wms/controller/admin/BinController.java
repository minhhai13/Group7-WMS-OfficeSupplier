package com.minhhai.wms.controller.admin;

import com.minhhai.wms.dto.BinDTO;
import com.minhhai.wms.entity.Bin;
import com.minhhai.wms.entity.User;
import com.minhhai.wms.entity.Warehouse;
import com.minhhai.wms.service.BinService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/warehouse/bins")
@RequiredArgsConstructor
public class BinController {

    private final BinService binService;

    private static final int PAGE_SIZE = 10;

    @GetMapping
    public String list(@RequestParam(name = "keyword", required = false) String keyword,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       Model model, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser.getWarehouse() == null) {
            model.addAttribute("bins", new ArrayList<Bin>());
            model.addAttribute("error", "Please contact the Admin to allocate inventory before managing Bins..");
            return "warehouse/bin-list";
        }
        Integer warehouseId = loggedInUser.getWarehouse().getWarehouseId();

        if (page < 0) page = 0;

        Page<Bin> binPage = binService.findPaginated(warehouseId, keyword, page, PAGE_SIZE);

        if (page > 0 && page >= binPage.getTotalPages()) {
            page = Math.max(0, binPage.getTotalPages() - 1);
            binPage = binService.findPaginated(warehouseId, keyword, page, PAGE_SIZE);
        }

        // Calculate weight info only for the current page's bins
        Map<Integer, BigDecimal> currentWeights = new LinkedHashMap<>();
        Map<Integer, BigDecimal> availableCapacities = new LinkedHashMap<>();
        for (Bin bin : binPage.getContent()) {
            currentWeights.put(bin.getBinId(), binService.getCurrentWeight(bin.getBinId()));
            availableCapacities.put(bin.getBinId(), binService.getAvailableCapacity(bin.getBinId()));
        }

        model.addAttribute("activePage", "warehouse-bins");
        model.addAttribute("binPage", binPage);
        model.addAttribute("bins", binPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", binPage.getTotalPages());
        model.addAttribute("totalElements", binPage.getTotalElements());
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentWeights", currentWeights);
        model.addAttribute("availableCapacities", availableCapacities);
        return "warehouse/bin-list";
    }

    @GetMapping("/new")
    public String createForm(Model model, HttpSession session) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser.getWarehouse() == null) {
            model.addAttribute("bins", new ArrayList<Bin>());
            model.addAttribute("error", "Please contact the Admin to allocate inventory before managing Bins.");
            return "warehouse/bin-list";
        }
        model.addAttribute("activePage", "warehouse-bins");
        model.addAttribute("bin", new BinDTO());
        return "warehouse/bin-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable(name = "id") Integer id,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        return binService.findById(id)
                .map(bin -> {
                    BinDTO dto = mapToDTO(bin);
                    model.addAttribute("activePage", "warehouse-bins");
                    model.addAttribute("bin", dto);
                    model.addAttribute("currentWeight", binService.getCurrentWeight(id));
                    model.addAttribute("availableCapacity", binService.getAvailableCapacity(id));
                    return "warehouse/bin-form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Bin not found.");
                    return "redirect:/warehouse/bins";
                });
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("bin") BinDTO binDTO,
                       BindingResult bindingResult,
                       HttpSession session,
                       Model model,
                       RedirectAttributes redirectAttributes) {

        User loggedInUser = (User) session.getAttribute("loggedInUser");
        Warehouse warehouse = loggedInUser.getWarehouse();

        // Pass warehouse info to DTO
        binDTO.setWarehouseId(warehouse.getWarehouseId());

        if (bindingResult.hasErrors()) {
            model.addAttribute("activePage", "warehouse-bins");
            if (binDTO.getBinId() != null) {
                model.addAttribute("currentWeight", binService.getCurrentWeight(binDTO.getBinId()));
                model.addAttribute("availableCapacity", binService.getAvailableCapacity(binDTO.getBinId()));
            }
            return "warehouse/bin-form";
        }

        try {
            binService.save(binDTO);
            redirectAttributes.addFlashAttribute("success", "Bin saved successfully.");
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage().toLowerCase();
            if (msg.contains("location") || msg.contains("exists")) {
                bindingResult.rejectValue("binLocation", "error.bin", e.getMessage());
            } else if (msg.contains("smaller")) {
                bindingResult.rejectValue("maxWeight", "error.weight", e.getMessage());
            } else {
                model.addAttribute("error", e.getMessage());
            }
            model.addAttribute("activePage", "warehouse-bins");
            if (binDTO.getBinId() != null) {
                model.addAttribute("currentWeight", binService.getCurrentWeight(binDTO.getBinId()));
                model.addAttribute("availableCapacity", binService.getAvailableCapacity(binDTO.getBinId()));
            }
            return "warehouse/bin-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/warehouse/bins";
        }

        return "redirect:/warehouse/bins";
    }

    @PostMapping("/{id}/toggle")
    public String toggleActive(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            binService.toggleActive(id);
            redirectAttributes.addFlashAttribute("success", "Bin status updated.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/warehouse/bins";
    }

    private BinDTO mapToDTO(Bin bin) {
        return BinDTO.builder()
                .binId(bin.getBinId())
                .binLocation(bin.getBinLocation())
                .maxWeight(bin.getMaxWeight())
                .warehouseId(bin.getWarehouse().getWarehouseId())
                .isActive(bin.getIsActive())
                .build();
    }
}
