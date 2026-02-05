package com.minhhai.wms.controller;

import com.minhhai.wms.dto.LoginRequest;
import com.minhhai.wms.entity.User;
import com.minhhai.wms.service.AuthService;
import com.minhhai.wms.service.AuthServiceImpl;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/login")
    public String loginForm(@ModelAttribute("loginRequest") LoginRequest loginRequest) {
        return "login";
    }

    @PostMapping("/login")
    public String login(
            @Valid @ModelAttribute("loginRequest") LoginRequest loginRequest,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "login";
        }

        Optional<User> user = authService.login(loginRequest);
        if (user.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Tên đăng nhập hoặc mật khẩu không đúng.");
            return "redirect:/login";
        }
        session.setAttribute(AuthServiceImpl.SESSION_USER, user.get());
        return "redirect:/dashboard";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        authService.logout(session);
        return "redirect:/login";
    }

    @GetMapping("/")
    public String index(HttpSession session) {
        if (session != null && session.getAttribute(AuthServiceImpl.SESSION_USER) != null) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }
}
