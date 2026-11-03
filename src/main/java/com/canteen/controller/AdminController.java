package com.canteen.controller;

import com.canteen.entity.DailySales;
import com.canteen.entity.MenuItem;
import com.canteen.entity.Order;
import com.canteen.entity.User;
import com.canteen.service.AdminService;
import com.canteen.service.LogService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Controller
public class AdminController {

    private final AdminService adminService;
    private final LogService logService;

    public AdminController(AdminService adminService, LogService logService) {
        this.adminService = adminService;
        this.logService = logService;
    }

    @GetMapping("/admin")
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        Integer stallId = user.getStallId();
        model.addAttribute("username", user.getUsername());
        model.addAttribute("stall", adminService.getStall(stallId));
        // dashboard summary
        model.addAttribute("pendingCount", adminService.getPendingCount(stallId));
        model.addAttribute("todayRevenue", adminService.getTodayRevenue(stallId));
        model.addAttribute("lowStock", adminService.getLowStock(stallId));
        // chart data: revenue over the last 7 days, split into two parallel lists
        // so Thymeleaf can drop them straight into the Chart.js script.
        List<DailySales> daily = adminService.getDailyRevenue(stallId);
        List<String> revLabels = new ArrayList<>();
        List<BigDecimal> revValues = new ArrayList<>();
        for (DailySales d : daily) {
            revLabels.add(d.getLabel());
            revValues.add(d.getRevenue());
        }
        model.addAttribute("revLabels", revLabels);
        model.addAttribute("revValues", revValues);
        model.addAttribute("statusCounts", adminService.getStatusCounts(stallId));
        return "admin/dashboard";
    }

    // ---- manage this vendor's own stall discount ----
    @GetMapping("/admin/discount")
    public String discountForm(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        model.addAttribute("stall", adminService.getStall(user.getStallId()));
        return "admin/discount";
    }

    @PostMapping("/admin/discount/update")
    public String updateDiscount(@RequestParam("discountPercent") BigDecimal discountPercent,
                                 HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        adminService.updateStallDiscount(user.getStallId(), discountPercent);
        logService.record(user.getUsername(), "DISCOUNT_UPDATE", "stallId=" + user.getStallId() + ", pct=" + discountPercent);
        return "redirect:/admin/discount";
    }

    @GetMapping("/admin/menu")
    public String menuManage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        model.addAttribute("items", adminService.getStallItems(user.getStallId()));
        return "admin/menu-manage";
    }

    @PostMapping("/admin/menu/add")
    public String addItem(@RequestParam("name") String name,
                          @RequestParam("description") String description,
                          @RequestParam("price") BigDecimal price,
                          @RequestParam("stock") Integer stock,
                          @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                          HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        MenuItem item = new MenuItem();
        item.setStallId(user.getStallId());
        item.setName(name);
        item.setDescription(description);
        item.setPrice(price);
        item.setStock(stock);
        item.setIsAvailable(true);
        item.setImageUrl(saveImage(imageFile));
        adminService.addItem(item);
        logService.record(user.getUsername(), "MENU_ADD", "item=" + name + ", stall=" + user.getStallId());
        return "redirect:/admin/menu";
    }

    @PostMapping("/admin/menu/edit")
    public String editItem(@RequestParam("id") Integer id,
                           @RequestParam("name") String name,
                           @RequestParam("description") String description,
                           @RequestParam("price") BigDecimal price,
                           @RequestParam("stock") Integer stock,
                           @RequestParam("imageUrl") String imageUrl,
                           @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                           HttpSession session) {
        MenuItem item = new MenuItem();
        item.setId(id);
        item.setName(name);
        item.setDescription(description);
        item.setPrice(price);
        item.setStock(stock);
        // if a new file was uploaded, use it; otherwise keep the old image
        String newImage = saveImage(imageFile);
        item.setImageUrl(newImage != null ? newImage : imageUrl);
        adminService.updateItem(item);
        User user2 = (User) session.getAttribute("loginUser");
        logService.record(user2.getUsername(), "MENU_EDIT", "itemId=" + id);
        return "redirect:/admin/menu";
    }

    @PostMapping("/admin/menu/delete")
    public String deleteItem(@RequestParam("id") Integer id, HttpSession session) {
        adminService.deleteItem(id);
        User user = (User) session.getAttribute("loginUser");
        logService.record(user.getUsername(), "MENU_DELETE", "itemId=" + id);
        return "redirect:/admin/menu";
    }

    @PostMapping("/admin/menu/toggle")
    public String toggleItem(@RequestParam("id") Integer id) {
        adminService.toggleAvailability(id);
        return "redirect:/admin/menu";
    }

    // Quick restock: add stock without editing the full item.
    @PostMapping("/admin/menu/restock")
    public String restock(@RequestParam("id") Integer id,
                          @RequestParam("qty") Integer qty,
                          HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        adminService.addStock(id, qty);
        logService.record(user.getUsername(), "MENU_RESTOCK", "itemId=" + id + ", qty=" + qty);
        return "redirect:/admin/menu";
    }

    // #10 open/close stall
    @PostMapping("/admin/stall/toggle-open")
    public String toggleStallOpen(HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        adminService.toggleStallOpen(user.getStallId());
        logService.record(user.getUsername(), "STALL_TOGGLE", "stallId=" + user.getStallId());
        return "redirect:/admin";
    }

    @GetMapping("/admin/orders")
    public String orderManage(@RequestParam(value = "status", required = false) String status,
                              HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        model.addAttribute("orders", adminService.getStallOrders(user.getStallId(), status));
        model.addAttribute("status", status);
        return "admin/order-manage";
    }

    // single transaction detail (only if this order contains the vendor's menu)
    @GetMapping("/admin/orders/{id}")
    public String orderDetail(@PathVariable("id") Integer id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        Order order = adminService.getStallOrder(id, user.getStallId());
        if (order == null) {
            return "redirect:/admin/orders";
        }
        model.addAttribute("order", order);
        model.addAttribute("items", adminService.getOrderItems(id));
        model.addAttribute("logs", adminService.getStatusLogs(id));
        return "admin/order-detail";
    }

    @PostMapping("/admin/orders/advance")
    public String advance(@RequestParam("id") Integer id, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        adminService.advanceStatus(id, user.getStallId());
        return "redirect:/admin/orders";
    }

    // #5 sales report for this stall
    @GetMapping("/admin/report")
    public String report(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        model.addAttribute("sales", adminService.getSales(user.getStallId()));
        return "admin/report";
    }

    // customer reviews for this stall's menu
    @GetMapping("/admin/reviews")
    public String reviews(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        model.addAttribute("reviews", adminService.getStallReviews(user.getStallId()));
        return "admin/reviews";
    }

    // Save the image file into the "uploads/" folder and return its URL (or null if empty).
    private String saveImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            Path dir = Paths.get("uploads");
            Files.createDirectories(dir);
            String original = file.getOriginalFilename();
            if (original == null) original = "image";
            original = original.replaceAll("[^a-zA-Z0-9._-]", "_");
            String filename = System.currentTimeMillis() + "_" + original;
            file.transferTo(dir.resolve(filename).toAbsolutePath());
            return "/uploads/" + filename;
        } catch (IOException e) {
            // if saving fails, don't break the flow; just continue without an image
            return null;
        }
    }
}
