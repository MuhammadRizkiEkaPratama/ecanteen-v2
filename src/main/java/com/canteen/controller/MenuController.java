package com.canteen.controller;

import com.canteen.entity.MenuItem;
import com.canteen.entity.Stall;
import com.canteen.entity.User;
import com.canteen.service.MenuService;
import com.canteen.service.OrderService;
import com.canteen.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class MenuController {

    private final MenuService menuService;
    private final ReviewService reviewService;
    private final OrderService orderService;

    public MenuController(MenuService menuService, ReviewService reviewService, OrderService orderService) {
        this.menuService = menuService;
        this.reviewService = reviewService;
        this.orderService = orderService;
    }

    @GetMapping("/menu")
    public String menu(@RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "stallId", required = false) Integer stallId,
                       HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) {
            return "redirect:/login";
        }

        List<Stall> allStalls = menuService.getActiveStalls();

        // if the user picked one stall in the filter, show only that stall
        List<Stall> stalls = new ArrayList<>();
        for (Stall stall : allStalls) {
            if (stallId == null || stall.getId().equals(stallId)) {
                stalls.add(stall);
            }
        }

        Map<Integer, List<MenuItem>> itemsByStall = new HashMap<>();
        for (Stall stall : stalls) {
            itemsByStall.put(stall.getId(), menuService.searchItemsByStall(stall.getId(), keyword));
        }

        model.addAttribute("username", user.getUsername());
        model.addAttribute("allStalls", allStalls);
        model.addAttribute("stalls", stalls);
        model.addAttribute("itemsByStall", itemsByStall);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStallId", stallId);
        // notification: how many orders are READY and waiting for pickup
        model.addAttribute("readyCount", orderService.countReadyOrders(user.getId()));
        return "menu";
    }

    @GetMapping("/menu/item/{id}")
    public String itemDetail(@PathVariable("id") Integer id,
                             HttpSession session, Model model) {
        MenuItem item = menuService.getItemById(id);
        // invalid / deleted item id -> back to the menu instead of crashing
        if (item == null) {
            return "redirect:/menu";
        }
        User user = (User) session.getAttribute("loginUser");
        model.addAttribute("item", item);
        model.addAttribute("reviews", reviewService.getReviews(id));
        model.addAttribute("avg", reviewService.getAverage(id));
        model.addAttribute("userId", user.getId());
        return "menu-item";
    }
}
