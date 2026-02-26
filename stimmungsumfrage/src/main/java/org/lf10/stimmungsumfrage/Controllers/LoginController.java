package org.lf10.stimmungsumfrage.Controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "login"; // login.html (Thymeleaf)
    }

    @GetMapping("/home")
    public String home() {
        return "home";
    }
}