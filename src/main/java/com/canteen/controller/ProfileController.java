package com.canteen.controller;

import com.canteen.entity.User;
import com.canteen.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        model.addAttribute("username", user.getUsername());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("role", user.getRole());
        return "profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(@RequestParam("oldPassword") String oldPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        boolean ok = userService.changePassword(user.getUsername(), oldPassword, newPassword);
        model.addAttribute("username", user.getUsername());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("role", user.getRole());
        if (ok) {
            model.addAttribute("message", "Your password has been changed.");
        } else {
            model.addAttribute("error", "Your old password is incorrect.");
        }
        return "profile";
    }
}
