package com.canteen.controller;

import com.canteen.entity.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // Root page "/". If not logged in -> go to /login.
    // If already logged in -> redirect to the home page for their role.
    @GetMapping("/")
    public String home(HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) {
            return "redirect:/login";
        }
        String role = user.getRole();
        if ("superadmin".equals(role)) {
            return "redirect:/superadmin";
        } else if ("admin".equals(role)) {
            return "redirect:/admin";
        } else if ("vendor".equals(role)) {
            return "redirect:/vendor";
        }
        return "redirect:/menu";
    }
}
