package com.minhhai.wms.controller;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/")
    public String index() {
        return "index"; // Sẽ tìm file /WEB-INF/templates/index.html
    }
}