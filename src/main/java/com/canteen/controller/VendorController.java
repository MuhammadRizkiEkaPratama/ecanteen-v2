package com.canteen.controller;

import com.canteen.entity.User;
import com.canteen.entity.VendorApplication;
import com.canteen.service.VendorService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class VendorController {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @GetMapping("/vendor")
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        VendorApplication app = vendorService.getApplicationForUser(user.getId());
        model.addAttribute("username", user.getUsername());
        // NOTE: don't name this "application" — that is reserved by Thymeleaf (application scope).
        model.addAttribute("vendorApp", app);
        return "vendor/dashboard";
    }

    @PostMapping("/vendor/apply")
    public String apply(@RequestParam("stallName") String stallName,
                        @RequestParam("stallDescription") String stallDescription,
                        @RequestParam("ownerName") String ownerName,
                        @RequestParam("phone") String phone,
                        HttpSession session, RedirectAttributes ra) {
        User user = (User) session.getAttribute("loginUser");
        String err = vendorService.apply(user.getId(), stallName, stallDescription, ownerName, phone);
        if (err != null) {
            ra.addFlashAttribute("error", err);
        } else {
            ra.addFlashAttribute("message", "Application submitted! Please wait for the administrator's approval.");
        }
        return "redirect:/vendor";
    }
}
