package com.canteen.controller;

import com.canteen.entity.User;
import com.canteen.service.LogService;
import com.canteen.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserService userService;
    private final LogService logService;

    public AuthController(UserService userService, LogService logService) {
        this.userService = userService;
        this.logService = logService;
    }

    @GetMapping("/register")
    public String registerForm(HttpSession session) {
        User already = (User) session.getAttribute("loginUser");
        if (already != null) {
            return "redirect:" + getHomePage(already.getRole());
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam("username") String username,
                           @RequestParam("email") String email,
                           @RequestParam("password") String password,
                           Model model) {
        boolean created = userService.register(username, email, password);
        if (!created) {
            model.addAttribute("error", "That username is already taken, please choose another.");
            return "register";
        }
        return "redirect:/login";
    }

    @GetMapping("/register-vendor")
    public String registerVendorForm(HttpSession session) {
        User already = (User) session.getAttribute("loginUser");
        if (already != null) {
            return "redirect:" + getHomePage(already.getRole());
        }
        return "register-vendor";
    }

    @PostMapping("/register-vendor")
    public String registerVendor(@RequestParam("username") String username,
                                 @RequestParam("email") String email,
                                 @RequestParam("password") String password,
                                 Model model) {
        boolean created = userService.registerVendor(username, email, password);
        if (!created) {
            model.addAttribute("error", "That username is already taken, please choose another.");
            return "register-vendor";
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginForm(@CookieValue(value = "rememberUser", required = false) String remembered,
                            HttpSession session, Model model) {
        User already = (User) session.getAttribute("loginUser");
        if (already != null) {
            return "redirect:" + getHomePage(already.getRole());
        }
        if (remembered != null) {
            model.addAttribute("rememberedUsername", remembered);
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam("username") String username,
                        @RequestParam("password") String password,
                        @RequestParam(value = "remember", required = false) String remember,
                        HttpSession session,
                        HttpServletResponse response,
                        Model model) {
        User user = userService.login(username, password);
        if (user == null) {
            logService.record(username, "LOGIN_FAILURE", "wrong username/password");
            model.addAttribute("error", "Incorrect username or password.");
            return "login";
        }
        // Accounts suspended by the administrator are not allowed in.
        if (Boolean.FALSE.equals(user.getIsActive())) {
            logService.record(username, "LOGIN_BLOCKED", "account suspended");
            model.addAttribute("error", "This account is currently disabled. Please contact the administrator.");
            return "login";
        }

        // "Remember username" cookie: keep for 30 days if checked, clear it otherwise.
        Cookie cookie = new Cookie("rememberUser", username);
        cookie.setPath("/");
        cookie.setMaxAge(remember != null ? 30 * 24 * 60 * 60 : 0);
        response.addCookie(cookie);

        session.setAttribute("loginUser", user);
        logService.record(username, "LOGIN_SUCCESS", "role=" + user.getRole());
        // Send each role to its own home page.
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

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    private String getHomePage(String role) {
        if ("superadmin".equals(role)) return "/superadmin";
        if ("admin".equals(role))      return "/admin";
        if ("vendor".equals(role))     return "/vendor";
        return "/menu";
    }
}
