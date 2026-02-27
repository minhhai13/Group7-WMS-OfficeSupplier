package com.minhhai.wms.controller;

import com.minhhai.wms.entity.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Global model attributes available to all Thymeleaf templates.
 * In Thymeleaf 3.1+ (Spring 6), direct session access is restricted,
 * so we expose user info via model attributes instead.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    @ModelAttribute("loggedInUserName")
    public String loggedInUserName(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user != null) {
            return user.getFullName() + " (" + user.getRole() + ")";
        }
        return null;
    }

    @ModelAttribute("loggedInUserRole")
    public String loggedInUserRole(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user != null) {
            return user.getRole();
        }
        return null;
    }
}
