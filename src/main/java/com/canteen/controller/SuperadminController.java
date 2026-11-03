package com.canteen.controller;

import com.canteen.entity.DailySales;
import com.canteen.entity.Order;
import com.canteen.entity.SalesReport;
import com.canteen.service.LogService;
import com.canteen.service.SuperadminService;
import com.canteen.service.VendorService;
import jakarta.servlet.http.HttpSession;
import com.canteen.entity.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SuperadminController {

    private final SuperadminService superadminService;
    private final LogService logService;
    private final VendorService vendorService;

    public SuperadminController(SuperadminService superadminService, LogService logService,
                                VendorService vendorService) {
        this.superadminService = superadminService;
        this.logService = logService;
        this.vendorService = vendorService;
    }

    @GetMapping("/superadmin")
    public String dashboard(Model model) {
        // statistik ringkas
        model.addAttribute("totalUsers", superadminService.getTotalUsers());
        model.addAttribute("totalStalls", superadminService.getTotalActiveStalls());
        model.addAttribute("totalRevenue", superadminService.getTotalRevenue());
        model.addAttribute("ordersToday", superadminService.getOrdersToday());

        // chart 1: revenue per day across the whole canteen (last 7 days)
        List<DailySales> daily = superadminService.getDailyRevenue();
        List<String> revLabels = new ArrayList<>();
        List<BigDecimal> revValues = new ArrayList<>();
        for (DailySales d : daily) {
            revLabels.add(d.getLabel());
            revValues.add(d.getRevenue());
        }
        model.addAttribute("revLabels", revLabels);
        model.addAttribute("revValues", revValues);

        // chart 2: revenue contributed by each stall
        List<SalesReport> perStall = superadminService.getSalesPerStall();
        List<String> stallLabels = new ArrayList<>();
        List<BigDecimal> stallValues = new ArrayList<>();
        for (SalesReport s : perStall) {
            stallLabels.add(s.getStallName());
            stallValues.add(s.getRevenue());
        }
        model.addAttribute("stallLabels", stallLabels);
        model.addAttribute("stallValues", stallValues);
        return "superadmin/dashboard";
    }

    // ---- manage users ----
    @GetMapping("/superadmin/users")
    public String users(Model model) {
        model.addAttribute("users", superadminService.getAllUsers());
        return "superadmin/users";
    }

    @PostMapping("/superadmin/users/toggle")
    public String toggleUser(@RequestParam("id") Integer id,
                             @RequestParam("active") Boolean active,
                             HttpSession session, RedirectAttributes ra) {
        superadminService.setUserActive(id, active);
        User sa = (User) session.getAttribute("loginUser");
        logService.record(sa.getUsername(), active ? "USER_ACTIVATED" : "USER_SUSPENDED", "targetId=" + id);
        ra.addFlashAttribute("message", "User has been " + (active ? "activated." : "suspended."));
        return "redirect:/superadmin/users";
    }

    @PostMapping("/superadmin/users/reset")
    public String resetUserPassword(@RequestParam("id") Integer id,
                                    @RequestParam("newPassword") String newPassword,
                                    HttpSession session, RedirectAttributes ra) {
        boolean ok = superadminService.resetUserPassword(id, newPassword);
        User sa = (User) session.getAttribute("loginUser");
        if (ok) {
            logService.record(sa.getUsername(), "USER_RESET_PW", "targetId=" + id);
            ra.addFlashAttribute("message", "Password has been reset.");
        } else {
            ra.addFlashAttribute("error", "Failed: the password was empty, or this account can't be reset.");
        }
        return "redirect:/superadmin/users";
    }

    // ---- suspend / activate stall ----
    @GetMapping("/superadmin/stalls")
    public String stalls(Model model) {
        model.addAttribute("stalls", superadminService.getAllStalls());
        return "superadmin/stalls";
    }

    @PostMapping("/superadmin/stalls/toggle")
    public String toggleStall(@RequestParam("id") Integer id,
                              @RequestParam("active") Boolean active,
                              HttpSession session, RedirectAttributes ra) {
        superadminService.setStallActive(id, active);
        User sa = (User) session.getAttribute("loginUser");
        logService.record(sa.getUsername(), active ? "STALL_ACTIVATED" : "STALL_SUSPENDED", "stallId=" + id);
        ra.addFlashAttribute("message", "Stall has been " + (active ? "activated." : "suspended."));
        return "redirect:/superadmin/stalls";
    }

    // ---- all canteen transactions ----
    @GetMapping("/superadmin/transactions")
    public String transactions(Model model) {
        List<Order> orders = superadminService.getAllOrders();

        // Load items for every order so the card view can show them inline.
        java.util.Map<Integer, List<com.canteen.entity.OrderItem>> itemsByOrder = new java.util.HashMap<>();
        for (Order o : orders) {
            itemsByOrder.put(o.getId(), superadminService.getOrderItems(o.getId()));
        }

        // Group orders from the same checkout into one card (same logic as OrderController).
        java.util.LinkedHashMap<Integer, List<Order>> grouped = new java.util.LinkedHashMap<>();
        for (Order o : orders) {
            Integer groupId = o.getOrderGroupId() != null ? o.getOrderGroupId() : o.getId();
            grouped.computeIfAbsent(groupId, k -> new java.util.ArrayList<>()).add(o);
        }
        List<List<Order>> groupList = new java.util.ArrayList<>(grouped.values());

        model.addAttribute("groupList", groupList);
        model.addAttribute("itemsByOrder", itemsByOrder);
        model.addAttribute("ordersEmpty", orders.isEmpty());
        return "superadmin/transactions";
    }

    @GetMapping("/superadmin/transactions/{id}")
    public String transactionDetail(@PathVariable("id") Integer id, Model model) {
        Order order = superadminService.getOrder(id);
        if (order == null) {
            return "redirect:/superadmin/transactions";
        }
        model.addAttribute("order", order);
        model.addAttribute("items", superadminService.getOrderItems(id));
        return "superadmin/transaction-detail";
    }

    // ---- review moderation ----
    @GetMapping("/superadmin/reviews")
    public String reviews(Model model) {
        model.addAttribute("reviews", superadminService.getAllReviews());
        return "superadmin/reviews";
    }

    @PostMapping("/superadmin/reviews/delete")
    public String deleteReview(@RequestParam("id") Integer id,
                               HttpSession session, RedirectAttributes ra) {
        superadminService.deleteReview(id);
        User sa = (User) session.getAttribute("loginUser");
        logService.record(sa.getUsername(), "REVIEW_DELETED", "reviewId=" + id);
        ra.addFlashAttribute("message", "Review has been deleted.");
        return "redirect:/superadmin/reviews";
    }

    @GetMapping("/superadmin/discounts")
    public String discounts(Model model) {
        model.addAttribute("discounts", superadminService.getAllDiscounts());
        model.addAttribute("stalls", superadminService.getAllStalls());
        return "superadmin/discounts";
    }

    @PostMapping("/superadmin/discounts/update")
    public String updateDiscount(@RequestParam("id") Integer id,
                                 @RequestParam("discountPercent") BigDecimal discountPercent,
                                 RedirectAttributes ra) {
        if (discountPercent.compareTo(BigDecimal.ZERO) < 0 || discountPercent.compareTo(new BigDecimal("100")) > 0) {
            ra.addFlashAttribute("error", "Discount percentage must be between 0 and 100.");
            return "redirect:/superadmin/discounts";
        }
        superadminService.updateDiscount(id, discountPercent);
        ra.addFlashAttribute("message", "Global discount has been updated.");
        return "redirect:/superadmin/discounts";
    }

    @PostMapping("/superadmin/discounts/toggle")
    public String toggleDiscount(@RequestParam("id") Integer id,
                                 @RequestParam("isActive") Boolean isActive,
                                 RedirectAttributes ra) {
        superadminService.setActive(id, isActive);
        ra.addFlashAttribute("message", "Discount has been " + (isActive ? "enabled." : "disabled."));
        return "redirect:/superadmin/discounts";
    }

    @PostMapping("/superadmin/stalls/discount")
    public String updateStallDiscount(@RequestParam("id") Integer id,
                                      @RequestParam("discountPercent") BigDecimal discountPercent,
                                      RedirectAttributes ra) {
        if (discountPercent.compareTo(BigDecimal.ZERO) < 0 || discountPercent.compareTo(new BigDecimal("100")) > 0) {
            ra.addFlashAttribute("error", "Discount percentage must be between 0 and 100.");
            return "redirect:/superadmin/discounts";
        }
        superadminService.updateStallDiscount(id, discountPercent);
        ra.addFlashAttribute("message", "Stall discount has been updated.");
        return "redirect:/superadmin/discounts";
    }

    @GetMapping("/superadmin/logs")
    public String logs(Model model) {
        model.addAttribute("logs", logService.getRecentLogs());
        return "superadmin/logs";
    }

    // ---- vendor approval ----
    @GetMapping("/superadmin/vendors")
    public String vendors(Model model) {
        model.addAttribute("applications", vendorService.getPendingApplications());
        return "superadmin/vendors";
    }

    @PostMapping("/superadmin/vendors/approve")
    public String approveVendor(@RequestParam("id") Integer id,
                                HttpSession session, RedirectAttributes ra) {
        vendorService.approve(id);
        User sa = (User) session.getAttribute("loginUser");
        logService.record(sa.getUsername(), "VENDOR_APPROVED", "applicationId=" + id);
        ra.addFlashAttribute("message", "Vendor approved. The stall has been created automatically.");
        return "redirect:/superadmin/vendors";
    }

    @PostMapping("/superadmin/vendors/reject")
    public String rejectVendor(@RequestParam("id") Integer id,
                               @RequestParam(value = "reason", required = false) String reason,
                               HttpSession session, RedirectAttributes ra) {
        vendorService.reject(id, reason);
        User sa = (User) session.getAttribute("loginUser");
        logService.record(sa.getUsername(), "VENDOR_REJECTED", "applicationId=" + id + ", reason=" + reason);
        ra.addFlashAttribute("message", "Vendor application has been rejected.");
        return "redirect:/superadmin/vendors";
    }

    // #5 sales report for the whole canteen
    @GetMapping("/superadmin/report")
    public String report(Model model) {
        model.addAttribute("total", superadminService.getSalesTotal());
        model.addAttribute("perStall", superadminService.getSalesPerStall());
        return "superadmin/report";
    }
}
