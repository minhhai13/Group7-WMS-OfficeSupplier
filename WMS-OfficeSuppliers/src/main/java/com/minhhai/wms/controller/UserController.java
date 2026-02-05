package com.minhhai.wms.controller;

import com.minhhai.wms.entity.User;
import com.minhhai.wms.service.UserService;
import com.minhhai.wms.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final WarehouseService warehouseService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userService.findAll());
        return "user/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("user", User.builder().build());
        prepareFormModel(model);
        return "user/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        Optional<User> opt = userService.findById(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy người dùng.");
            return "redirect:/admin/users";
        }
        model.addAttribute("user", opt.get());
        prepareFormModel(model);
        return "user/form";
    }

    @PostMapping("/save")
    public String save(
            @Valid @ModelAttribute("user") User user,
            BindingResult bindingResult,
            @RequestParam(value = "plainPassword", required = false) String plainPassword,
            @RequestParam(value = "warehouseId", required = false) Integer warehouseId,
            Model model,
            RedirectAttributes redirectAttributes) {

        Integer id = user.getUserId();
        if (id == null && (plainPassword == null || plainPassword.isBlank())) {
            bindingResult.rejectValue("passwordHash", "required", "Mật khẩu bắt buộc khi tạo mới.");
        }

        String username = user.getUsername();
        if (username != null && !username.isBlank()) {
            boolean duplicate = (id == null)
                    ? userService.existsByUsername(username)
                    : userService.existsByUsernameExcludingId(username, id);
            if (duplicate) {
                bindingResult.rejectValue("username", "duplicate", "Tên đăng nhập đã tồn tại.");
            }
        }

        if (userService.getWarehouseRequiredRoles().contains(user.getRole()) && warehouseId != null) {
            user.setWarehouse(warehouseService.findById(warehouseId).orElse(null));
        } else {
            user.setWarehouse(null);
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("user", user);
            prepareFormModel(model);
            return "user/form";
        }

        userService.save(user, plainPassword);
        redirectAttributes.addFlashAttribute("success", id == null ? "Thêm người dùng thành công." : "Cập nhật người dùng thành công.");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        if (userService.findById(id).isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy người dùng.");
            return "redirect:/admin/users";
        }
        userService.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Đã xóa người dùng.");
        return "redirect:/admin/users";
    }

    private void prepareFormModel(Model model) {
        model.addAttribute("roleOptions", userService.getRoleOptions());
        model.addAttribute("warehouseRequiredRoles", userService.getWarehouseRequiredRoles());
        model.addAttribute("warehouseRequiredRolesStr", String.join("|", userService.getWarehouseRequiredRoles()));
        model.addAttribute("warehouses", warehouseService.findAll());
    }
}
