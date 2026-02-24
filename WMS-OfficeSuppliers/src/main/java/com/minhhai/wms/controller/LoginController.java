package com.minhhai.wms.controller;

import com.minhhai.wms.dto.LoginDTO;
import com.minhhai.wms.entity.User;
import com.minhhai.wms.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class LoginController {

    private final UserService userService;

    @GetMapping("/")
    public String home(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return "redirect:/login";
        }
        return redirectByRole(user.getRole());
    }

    @GetMapping("/login")
    public String loginPage(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user != null) {
            return redirectByRole(user.getRole());
        }
        // Thêm DTO vào model để ràng buộc với form
        model.addAttribute("loginDTO", new LoginDTO());
        return "login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("loginDTO") LoginDTO loginDTO,
                        BindingResult bindingResult,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {

        // Kiểm tra lỗi validation (ví dụ: để trống trường)
        if (bindingResult.hasErrors()) {
            return "login";
        }

        Optional<User> userOpt = userService.authenticate(loginDTO.getUsername(), loginDTO.getPassword());
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Invalid username or password.");
            return "redirect:/login";
        }

        User user = userOpt.get();
        session.setAttribute("loggedInUser", user);
        return redirectByRole(user.getRole());
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/403")
    public String accessDenied(Model model) {
        return "error/403";
    }

    private String redirectByRole(String role) {
        return switch (role) {
            case "System Admin" -> "redirect:/admin/dashboard";
            case "Warehouse Admin" -> "redirect:/warehouse/dashboard";
            case "Purchasing Staff" -> "redirect:/purchasing/po-list";
            case "Purchasing Manager" -> "redirect:/purchasing/approval-dashboard";
            case "Storekeeper" -> "redirect:/storekeeper/grn-list";
            default -> "redirect:/login";
        };
    }
}
