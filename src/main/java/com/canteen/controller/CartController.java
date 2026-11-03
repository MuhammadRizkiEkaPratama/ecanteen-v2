package com.canteen.controller;

import com.canteen.entity.User;
import com.canteen.service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam("menuItemId") Integer menuItemId,
                            HttpSession session, RedirectAttributes ra) {
        User user = (User) session.getAttribute("loginUser");
        String error = cartService.addToCart(user.getId(), menuItemId);
        if (error != null) {
            ra.addFlashAttribute("error", error);
        }
        return "redirect:/menu";
    }

    @GetMapping("/cart")
    public String viewCart(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        model.addAttribute("items", cartService.getCartView(user.getId()));
        model.addAttribute("subtotal", cartService.getSubtotal(user.getId()));
        return "cart";
    }

    @PostMapping("/cart/update")
    public String updateQuantity(@RequestParam("id") Integer id,
                                 @RequestParam("quantity") Integer quantity,
                                 HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        cartService.updateQuantity(user.getId(), id, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String removeItem(@RequestParam("id") Integer id, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        cartService.removeItem(user.getId(), id);
        return "redirect:/cart";
    }
}
