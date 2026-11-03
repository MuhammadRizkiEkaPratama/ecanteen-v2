package com.canteen.controller;

import com.canteen.entity.Order;
import com.canteen.entity.OrderItem;
import com.canteen.entity.User;
import com.canteen.service.CartService;
import com.canteen.service.LogService;
import com.canteen.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;
    private final LogService logService;

    public OrderController(OrderService orderService, CartService cartService, LogService logService) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.logService = logService;
    }

    @GetMapping("/order/checkout")
    public String checkout(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        model.addAttribute("items", cartService.getCartView(user.getId()));
        // preview the discount calculation so the customer sees the total before confirming
        java.util.Map<String, java.math.BigDecimal> totals = orderService.calcCheckoutTotals(user.getId());
        model.addAttribute("previewSubtotal",      totals.get("subtotal"));
        model.addAttribute("previewStallDiscount", totals.get("stallDiscount"));
        model.addAttribute("previewGlobalDiscount",totals.get("globalDiscount"));
        model.addAttribute("previewTotal",         totals.get("total"));
        return "checkout";
    }

    @PostMapping("/order/place")
    public String place(@RequestParam("paymentMethod") String paymentMethod,
                        HttpSession session, RedirectAttributes ra) {
        User user = (User) session.getAttribute("loginUser");

        String stockError = orderService.getStockError(user.getId());
        if (stockError != null) {
            ra.addFlashAttribute("error", stockError);
            return "redirect:/cart";
        }

        List<Integer> orderIds = orderService.checkout(user.getId(), paymentMethod);
        if (orderIds.isEmpty()) {
            ra.addFlashAttribute("error", "Failed to place the order. Please try again.");
            return "redirect:/cart";
        }
        // Store full batch so confirmation can show all stall orders
        session.setAttribute("lastOrderBatch", orderIds);
        logService.record(user.getUsername(), "ORDER_PLACED", "orderIds=" + orderIds);
        return "redirect:/order/confirmation/" + orderIds.get(0);
    }

    @GetMapping("/order/confirmation/{id}")
    public String confirmation(@PathVariable("id") Integer id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");

        @SuppressWarnings("unchecked")
        List<Integer> batch = (List<Integer>) session.getAttribute("lastOrderBatch");
        session.removeAttribute("lastOrderBatch");

        if (batch == null || batch.isEmpty()) {
            batch = new java.util.ArrayList<>();
            batch.add(id);
        }

        List<Order> orders = new java.util.ArrayList<>();
        Map<Integer, List<OrderItem>> itemsByOrder = new HashMap<>();
        for (Integer orderId : batch) {
            Order o = orderService.getOrder(orderId);
            // only show orders that belong to the logged-in user (block IDOR)
            if (o != null && o.getUserId().equals(user.getId())) {
                orders.add(o);
                itemsByOrder.put(orderId, orderService.getOrderItems(orderId));
            }
        }
        // nothing the user is allowed to see -> send them to their history
        if (orders.isEmpty()) {
            return "redirect:/order/history";
        }
        model.addAttribute("orders", orders);
        model.addAttribute("itemsByOrder", itemsByOrder);
        return "order-confirmation";
    }

    // #3 detail + real-time status (status is changed by the vendor/admin)
    @GetMapping("/order/detail/{id}")
    public String detail(@PathVariable("id") Integer id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        Order order = orderService.getOrder(id);
        // only the owner of the order may view it
        if (order == null || !order.getUserId().equals(user.getId())) {
            return "redirect:/order/history";
        }
        model.addAttribute("order", order);
        model.addAttribute("items", orderService.getOrderItems(id));
        model.addAttribute("logs", orderService.getStatusLogs(id));
        String st = order.getStatus();
        boolean canReview = "READY".equals(st) || "PICKED_UP".equals(st);
        model.addAttribute("pickedUp", canReview);
        return "order-detail";
    }

    @GetMapping("/order/history")
    public String history(@RequestParam(value = "status", required = false) String status,
                          HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        List<Order> orders = orderService.getOrderHistoryFiltered(user.getId(), status);

        Map<Integer, List<OrderItem>> itemsByOrder = new HashMap<>();
        for (Order order : orders) {
            itemsByOrder.put(order.getId(), orderService.getOrderItems(order.getId()));
        }

        // Group orders that came from the same checkout into one card.
        // Old orders without a group id fall back to their own id (= their own group).
        java.util.LinkedHashMap<Integer, List<Order>> grouped = new java.util.LinkedHashMap<>();
        for (Order order : orders) {
            Integer groupId = order.getOrderGroupId() != null ? order.getOrderGroupId() : order.getId();
            grouped.computeIfAbsent(groupId, k -> new java.util.ArrayList<>()).add(order);
        }
        List<List<Order>> groupList = new java.util.ArrayList<>(grouped.values());

        model.addAttribute("groupList", groupList);
        model.addAttribute("itemsByOrder", itemsByOrder);
        model.addAttribute("ordersEmpty", orders.isEmpty());
        model.addAttribute("status", status);
        model.addAttribute("readyCount", orderService.countReadyOrders(user.getId()));
        return "order-history";
    }

    // Repeat order: add all items from a past order back into the cart.
    @PostMapping("/order/repeat")
    public String repeat(@RequestParam("id") Integer id,
                         HttpSession session, RedirectAttributes ra) {
        User user = (User) session.getAttribute("loginUser");
        String err = orderService.repeatOrder(id, user.getId());
        if (err != null) {
            ra.addFlashAttribute("error", err);
            return "redirect:/order/history";
        }
        ra.addFlashAttribute("message", "Items from order #" + id + " have been added to your cart.");
        return "redirect:/cart";
    }

    // Customer cancels an order that is still PENDING.
    @PostMapping("/order/cancel")
    public String cancel(@RequestParam("id") Integer id,
                         HttpSession session, RedirectAttributes ra) {
        User user = (User) session.getAttribute("loginUser");
        boolean ok = orderService.cancelOrder(id, user.getId());
        if (ok) {
            ra.addFlashAttribute("message", "Order #" + id + " has been cancelled.");
            logService.record(user.getUsername(), "ORDER_CANCELLED", "orderId=" + id);
        } else {
            ra.addFlashAttribute("error", "This order can't be cancelled (already processed or not yours).");
        }
        return "redirect:/order/history";
    }
}
