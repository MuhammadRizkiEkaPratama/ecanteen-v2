package com.canteen.service;

import com.canteen.entity.CartView;
import com.canteen.entity.DailySales;
import com.canteen.entity.Discount;
import com.canteen.entity.MenuItem;
import com.canteen.entity.Order;
import com.canteen.entity.OrderItem;
import com.canteen.entity.OrderStatusLog;
import com.canteen.entity.SalesReport;
import com.canteen.mapper.DiscountMapper;
import com.canteen.mapper.MenuMapper;
import com.canteen.mapper.OrderMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final DiscountMapper discountMapper;
    private final CartService cartService;
    private final MenuMapper menuMapper;

    public OrderService(OrderMapper orderMapper, DiscountMapper discountMapper,
                        CartService cartService, MenuMapper menuMapper) {
        this.orderMapper = orderMapper;
        this.discountMapper = discountMapper;
        this.cartService = cartService;
        this.menuMapper = menuMapper;
    }

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    // #2 Validate stock before payment. Returns an error message, or null if all is fine.
    public String getStockError(Integer userId) {
        List<CartView> rows = cartService.getCartView(userId);
        if (rows.isEmpty()) {
            return "Your cart is empty.";
        }
        for (CartView row : rows) {
            MenuItem item = menuMapper.findItemById(row.getMenuItemId());
            if (item == null || !Boolean.TRUE.equals(item.getIsAvailable())) {
                return "\"" + row.getItemName() + "\" is no longer available.";
            }
            if (item.getStock() == null || item.getStock() < row.getQuantity()) {
                return "Not enough stock for \"" + row.getItemName() + "\" (only "
                        + (item.getStock() == null ? 0 : item.getStock()) + ").";
            }
        }
        return null;
    }

    // Creates one order per stall. Returns list of all order IDs created (one per stall),
    // or empty list if the cart is empty / stock check fails.
    public List<Integer> checkout(Integer userId, String paymentMethod) {
        List<CartView> rows = cartService.getCartView(userId);
        if (rows.isEmpty()) return new java.util.ArrayList<>();
        if (getStockError(userId) != null) return new java.util.ArrayList<>();

        // Group cart items by stall
        java.util.Map<Integer, List<CartView>> byStall = new java.util.LinkedHashMap<>();
        for (CartView row : rows) {
            byStall.computeIfAbsent(row.getStallId(), k -> new java.util.ArrayList<>()).add(row);
        }

        BigDecimal adminPercent = BigDecimal.ZERO;
        Discount active = discountMapper.findActiveDiscount();
        if (active != null) adminPercent = active.getDiscountPercent();

        List<Integer> orderIds = new java.util.ArrayList<>();

        for (java.util.Map.Entry<Integer, List<CartView>> entry : byStall.entrySet()) {
            Integer stallId = entry.getKey();
            List<CartView> stallRows = entry.getValue();

            // 1) subtotal and per-stall discount for this stall's items only
            BigDecimal subtotal = BigDecimal.ZERO;
            BigDecimal stallDiscount = BigDecimal.ZERO;
            for (CartView row : stallRows) {
                BigDecimal lineTotal = row.getUnitPrice().multiply(BigDecimal.valueOf(row.getQuantity()));
                subtotal = subtotal.add(lineTotal);
                BigDecimal lineStallDiscount = lineTotal.multiply(row.getStallDiscountPercent())
                        .divide(HUNDRED, 2, RoundingMode.HALF_UP);
                stallDiscount = stallDiscount.add(lineStallDiscount);
            }

            // 2) global discount applied to this stall's sub-total
            BigDecimal afterStall = subtotal.subtract(stallDiscount);
            BigDecimal adminDiscount = afterStall.multiply(adminPercent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
            BigDecimal discountTotal = stallDiscount.add(adminDiscount).setScale(2, RoundingMode.HALF_UP);
            BigDecimal total = subtotal.subtract(discountTotal).setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);

            // 3) save order header — one order per stall
            Order order = new Order();
            order.setUserId(userId);
            order.setStallId(stallId);
            order.setSubtotal(subtotal);
            order.setDiscountTotal(discountTotal);
            order.setTotalAmount(total);
            order.setPaymentMethod(paymentMethod);
            order.setStatus("PENDING");
            orderMapper.insertOrder(order);

            // 4) save frozen line items + decrease stock
            for (CartView row : stallRows) {
                OrderItem item = new OrderItem();
                item.setOrderId(order.getId());
                item.setMenuItemId(row.getMenuItemId());
                item.setItemName(row.getItemName());
                item.setUnitPrice(row.getUnitPrice());
                item.setQuantity(row.getQuantity());
                orderMapper.insertOrderItem(item);
                menuMapper.decreaseStock(row.getMenuItemId(), row.getQuantity());
            }

            addStatusLog(order.getId(), "PENDING");
            orderIds.add(order.getId());
        }

        // Tag every order from this checkout with the same group id (= first order id),
        // so Order History can show them together as one card.
        Integer groupId = orderIds.get(0);
        for (Integer oid : orderIds) {
            orderMapper.updateOrderGroup(oid, groupId);
        }

        cartService.clearCart(userId);
        return orderIds;
    }

    public List<Order> getOrderHistory(Integer userId) {
        return orderMapper.findOrdersByUser(userId);
    }

    // Order history with an optional status filter (null = all).
    public List<Order> getOrderHistoryFiltered(Integer userId, String status) {
        return orderMapper.findOrdersByUserFiltered(userId, status);
    }

    public List<OrderItem> getOrderItems(Integer orderId) {
        return orderMapper.findItemsByOrder(orderId);
    }

    public Order getOrder(Integer orderId) {
        return orderMapper.findOrderById(orderId);
    }

    public List<Order> getAllOrders() {
        return orderMapper.findAllOrders();
    }

    // Orders for a single stall only (used by the vendor). status null = all.
    public List<Order> getOrdersByStall(Integer stallId, String status) {
        return orderMapper.findOrdersByStall(stallId, status);
    }

    public int countOrdersByStallAndStatus(Integer stallId, String status) {
        return orderMapper.countOrdersByStallAndStatus(stallId, status);
    }

    public BigDecimal getTodayRevenueByStall(Integer stallId) {
        return orderMapper.todayRevenueByStall(stallId);
    }

    public int countOrdersToday() {
        return orderMapper.countOrdersToday();
    }

    public List<OrderStatusLog> getStatusLogs(Integer orderId) {
        return orderMapper.findStatusLogsByOrder(orderId);
    }

    // Plain status step: PENDING -> PROCESSING -> READY -> PICKED_UP (no State pattern).
    public void advanceStatus(Integer orderId) {
        Order order = orderMapper.findOrderById(orderId);
        if (order == null) {
            return;
        }
        String current = order.getStatus();
        String next;
        switch (current) {
            case "PENDING":    next = "PROCESSING"; break;
            case "PROCESSING": next = "READY";      break;
            case "READY":      next = "PICKED_UP";  break;
            default:           next = current;      break; // PICKED_UP stays
        }
        if (!next.equals(current)) {
            orderMapper.updateStatus(orderId, next);
            addStatusLog(orderId, next);
        }
    }

    public boolean orderBelongsToStall(Integer orderId, Integer stallId) {
        return orderMapper.countOrderInStall(orderId, stallId) > 0;
    }

    // A vendor may only advance orders that contain their own stall's menu.
    public void advanceStatusForStall(Integer orderId, Integer stallId) {
        if (orderMapper.countOrderInStall(orderId, stallId) == 0) {
            return; // not this stall's order -> ignore
        }
        advanceStatus(orderId);
    }

    private void addStatusLog(Integer orderId, String status) {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(orderId);
        log.setStatus(status);
        orderMapper.insertStatusLog(log);
    }

    // Cancel a PENDING order. Stock is restored. Returns false if it can't be cancelled.
    public boolean cancelOrder(Integer orderId, Integer userId) {
        Order order = orderMapper.findOrderById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            return false;
        }
        if (!"PENDING".equals(order.getStatus())) {
            return false; // already processed, can't be cancelled
        }
        // restore stock for each item
        List<OrderItem> items = orderMapper.findItemsByOrder(orderId);
        for (OrderItem item : items) {
            menuMapper.restoreStock(item.getMenuItemId(), item.getQuantity());
        }
        orderMapper.updateStatus(orderId, "CANCELLED");
        addStatusLog(orderId, "CANCELLED");
        return true;
    }

    // Number of the user's orders that are READY (for the navbar badge).
    public int countReadyOrders(Integer userId) {
        return orderMapper.countReadyOrders(userId);
    }

    // Compute the checkout preview totals (subtotal, stall discount, global discount, total).
    // Used by the checkout page so the customer sees the total before confirming.
    public java.util.Map<String, BigDecimal> calcCheckoutTotals(Integer userId) {
        List<CartView> rows = cartService.getCartView(userId);
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal stallDiscount = BigDecimal.ZERO;
        for (CartView row : rows) {
            BigDecimal lineTotal = row.getUnitPrice().multiply(BigDecimal.valueOf(row.getQuantity()));
            subtotal = subtotal.add(lineTotal);
            BigDecimal lineStallDiscount = lineTotal.multiply(row.getStallDiscountPercent())
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);
            stallDiscount = stallDiscount.add(lineStallDiscount);
        }
        BigDecimal afterStall = subtotal.subtract(stallDiscount);
        BigDecimal adminPercent = BigDecimal.ZERO;
        Discount active = discountMapper.findActiveDiscount();
        if (active != null) {
            adminPercent = active.getDiscountPercent();
        }
        BigDecimal globalDiscount = afterStall.multiply(adminPercent).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.subtract(stallDiscount).subtract(globalDiscount)
                .setScale(2, RoundingMode.HALF_UP);

        java.util.Map<String, BigDecimal> map = new java.util.LinkedHashMap<>();
        map.put("subtotal",       subtotal.setScale(2, RoundingMode.HALF_UP));
        map.put("stallDiscount",  stallDiscount.setScale(2, RoundingMode.HALF_UP));
        map.put("globalDiscount", globalDiscount.setScale(2, RoundingMode.HALF_UP));
        map.put("discountTotal",  stallDiscount.add(globalDiscount).setScale(2, RoundingMode.HALF_UP));
        map.put("total",          total);
        return map;
    }

    // Repeat order: add all items from a past order back into the cart.
    // Returns an error message, or null if at least one item was added.
    public String repeatOrder(Integer orderId, Integer userId) {
        Order order = orderMapper.findOrderById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            return "Order not found.";
        }
        List<OrderItem> items = orderMapper.findItemsByOrder(orderId);
        int added = 0;
        for (OrderItem item : items) {
            String err = cartService.addToCart(userId, item.getMenuItemId());
            if (err == null) added++;
        }
        if (added == 0) {
            return "All items are unavailable or the stall is closed.";
        }
        return null;
    }

    // ---- #5 sales report ----
    public SalesReport salesByStall(Integer stallId) {
        return orderMapper.salesByStall(stallId);
    }

    public List<SalesReport> salesPerStall() {
        return orderMapper.salesPerStall();
    }

    public SalesReport salesTotal() {
        return orderMapper.salesTotal();
    }

    // ---- dashboard charts ----
    // Revenue for one stall, last 7 days, with empty days filled in as 0.
    public List<DailySales> dailyRevenueByStall(Integer stallId) {
        return fillLast7Days(orderMapper.dailyRevenueByStall(stallId));
    }

    // Revenue for the whole canteen, last 7 days, empty days filled in as 0.
    public List<DailySales> dailyRevenueAll() {
        return fillLast7Days(orderMapper.dailyRevenueAll());
    }

    // The SQL only returns days that actually had sales. For a clean chart we
    // want all 7 days in order, so we build the date list ourselves and copy in
    // any revenue the query found for that date. Plain loops, no patterns.
    private List<DailySales> fillLast7Days(List<DailySales> rows) {
        java.util.Map<String, BigDecimal> found = new java.util.HashMap<>();
        for (DailySales row : rows) {
            found.put(row.getLabel(), row.getRevenue());
        }
        List<DailySales> result = new java.util.ArrayList<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            java.time.LocalDate day = today.minusDays(i);
            String key = day.toString(); // yyyy-MM-dd, matches the SQL label
            BigDecimal revenue = found.getOrDefault(key, BigDecimal.ZERO);
            // Short label shown on the chart, e.g. "13 Jun"
            String shortLabel = day.getDayOfMonth() + " "
                    + day.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH);
            result.add(new DailySales(shortLabel, revenue));
        }
        return result;
    }
}
