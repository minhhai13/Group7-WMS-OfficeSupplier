package com.minhhai.wms.controller;

import com.minhhai.wms.dto.ProfileDTO;
import com.minhhai.wms.entity.User;
import com.minhhai.wms.service.UserService;
import com.minhhai.wms.util.PasswordUtil;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping
    public String viewProfile(HttpSession session, Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        // Fetch fresh user data
        User user = userService.findById(loggedInUser.getUserId()).orElse(loggedInUser);
        
        ProfileDTO profileDTO = ProfileDTO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .build();

        model.addAttribute("profile", profileDTO);
        model.addAttribute("role", user.getRole());
        return "profile";
    }

    @PostMapping("/save")
    public String saveProfile(@Valid @ModelAttribute("profile") ProfileDTO profileDTO,
                              BindingResult bindingResult,
                              HttpSession session,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("role", loggedInUser.getRole());
            return "profile";
        }

        try {
            User user = userService.findById(loggedInUser.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

            if (profileDTO.getCurrentPassword() == null || profileDTO.getCurrentPassword().isEmpty()) {
                bindingResult.rejectValue("currentPassword", "error.profile", "Please enter your current password to save changes.");
                model.addAttribute("role", loggedInUser.getRole());
                return "profile";
            }
            if (!PasswordUtil.verifyPassword(profileDTO.getCurrentPassword(), user.getPasswordHash())) {
                bindingResult.rejectValue("currentPassword", "error.profile", "Incorrect current password.");
                model.addAttribute("role", loggedInUser.getRole());
                return "profile";
            }

            // Check if username is changing and available
            if (!user.getUsername().equals(profileDTO.getUsername())) {
                userService.search(profileDTO.getUsername()).stream()
                    .filter(u -> u.getUsername().equalsIgnoreCase(profileDTO.getUsername()))
                    .findFirst()
                    .ifPresent(u -> {
                        throw new IllegalArgumentException("Username already exists.");
                    });
            }

            user.setUsername(profileDTO.getUsername());
            user.setFullName(profileDTO.getFullName());

            if (profileDTO.getPassword() != null && !profileDTO.getPassword().trim().isEmpty()) {
                user.setPasswordHash(PasswordUtil.hashPassword(profileDTO.getPassword().trim()));
            }

            user = userService.save(user);
            
            // Update session with new user data
            session.setAttribute("loggedInUser", user);
            
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully.");
        } catch (IllegalArgumentException e) {
            if (e.getMessage().toLowerCase().contains("username")) {
                bindingResult.rejectValue("username", "error.profile", e.getMessage());
            } else {
                model.addAttribute("error", e.getMessage());
            }
            model.addAttribute("role", loggedInUser.getRole());
            return "profile";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }

        return "redirect:/profile";
    }
}
