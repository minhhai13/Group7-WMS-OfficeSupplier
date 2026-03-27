package com.minhhai.wms.controller.admin;

import com.minhhai.wms.dto.UserDTO;
import com.minhhai.wms.entity.User;
import com.minhhai.wms.service.UserService;
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
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final WarehouseService warehouseService;

    private static final String[] ROLES = {
            "System Admin", "Warehouse Admin", "Warehouse Manager",
            "Purchasing Manager", "Purchasing Staff",
            "Sales Manager", "Sales Staff", "Storekeeper"
    };

    @GetMapping
    public String list(@RequestParam(name = "keyword", required = false) String keyword,
                       Model model) {
        List<User> users;
        if (keyword != null && !keyword.isBlank()) {
            users = userService.search(keyword);
            model.addAttribute("keyword", keyword);
        } else {
            users = userService.findAll();
        }
        model.addAttribute("activePage", "admin-users");
        model.addAttribute("users", users);
        return "admin/user-list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("activePage", "admin-users");
        model.addAttribute("user", new UserDTO());
        model.addAttribute("roles", ROLES);
        model.addAttribute("warehouses", warehouseService.findAllActive());
        return "admin/user-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable(name = "id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        return userService.findById(id)
                .map(user -> {
                    UserDTO userDTO = mapToDTO(user);
                    model.addAttribute("activePage", "admin-users");
                    model.addAttribute("user", userDTO);
                    model.addAttribute("roles", ROLES);
                    model.addAttribute("warehouses", warehouseService.findAllActive());
                    return "admin/user-form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "User not found.");
                    return "redirect:/admin/users";
                });
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("user") UserDTO userDTO,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        
        // Return to form if validation fails
        if (bindingResult.hasErrors()) {
            model.addAttribute("activePage", "admin-users");
            model.addAttribute("roles", ROLES);
            model.addAttribute("warehouses", warehouseService.findAllActive());
            return "admin/user-form";
        }

        try {
            userService.save(userDTO);
            redirectAttributes.addFlashAttribute("success", "User saved successfully.");
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage().toLowerCase();

            if (msg.contains("username")) {
                bindingResult.rejectValue("username", "error.user", e.getMessage());
            } else if (msg.contains("password")) {
                bindingResult.rejectValue("password", "error.user", e.getMessage());
            } else {
                model.addAttribute("error", e.getMessage());
            }
            model.addAttribute("activePage", "admin-users");
            model.addAttribute("roles", ROLES);
            model.addAttribute("warehouses", warehouseService.findAllActive());
            return "admin/user-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/admin/users";
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/toggle")
    public String toggleActive(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            userService.toggleActive(id);
            redirectAttributes.addFlashAttribute("success", "User status updated.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error occurred: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    private UserDTO mapToDTO(User user) {
        return UserDTO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole())
                .warehouseId(user.getWarehouse() != null ? user.getWarehouse().getWarehouseId() : null)
                .isActive(user.getIsActive())
                .build();
    }
}
