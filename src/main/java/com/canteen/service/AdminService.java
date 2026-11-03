package com.canteen.service;

import com.canteen.entity.DailySales;
import com.canteen.entity.MenuItem;
import com.canteen.entity.Order;
import com.canteen.entity.OrderItem;
import com.canteen.entity.OrderStatusLog;
import com.canteen.entity.Review;
import com.canteen.entity.SalesReport;
import com.canteen.entity.Stall;
import com.canteen.mapper.MenuMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AdminService {

    private final MenuMapper menuMapper;
    private final OrderService orderService;
    private final ReviewService reviewService;

    public AdminService(MenuMapper menuMapper, OrderService orderService, ReviewService reviewService) {
        this.menuMapper = menuMapper;
        this.orderService = orderService;
        this.reviewService = reviewService;
    }

    // ---- menu management, scoped to the admin's own stall ----
    public List<MenuItem> getStallItems(Integer stallId) {
        return menuMapper.findItemsByStall(stallId);
    }

    public void addItem(MenuItem item) {
        menuMapper.insertItem(item);
    }

    public void updateItem(MenuItem item) {
        menuMapper.updateItem(item);
    }

    public void deleteItem(Integer id) {
        menuMapper.deleteItem(id);
    }

    // Flip the current availability (sold out <-> available).
    public void toggleAvailability(Integer id) {
        MenuItem item = menuMapper.findItemById(id);
        if (item != null) {
            menuMapper.toggleAvailability(id, !item.getIsAvailable());
        }
    }

    // ---- #10 open/close stall ----
    public Stall getStall(Integer stallId) {
        return menuMapper.findStallById(stallId);
    }

    public void toggleStallOpen(Integer stallId) {
        Stall stall = menuMapper.findStallById(stallId);
        if (stall != null) {
            menuMapper.toggleStallOpen(stallId, !stall.getIsOpen());
        }
    }

    // ---- vendor's own stall discount ----
    public void updateStallDiscount(Integer stallId, BigDecimal discountPercent) {
        menuMapper.updateStallDiscount(stallId, discountPercent);
    }

    // ---- orders (only this vendor's stall orders) ----
    public List<Order> getStallOrders(Integer stallId, String status) {
        return orderService.getOrdersByStall(stallId, status);
    }

    public void advanceStatus(Integer orderId, Integer stallId) {
        orderService.advanceStatusForStall(orderId, stallId);
    }

    // single order detail, only if the order actually contains this stall's menu
    public Order getStallOrder(Integer orderId, Integer stallId) {
        if (!orderService.orderBelongsToStall(orderId, stallId)) {
            return null;
        }
        return orderService.getOrder(orderId);
    }

    public List<OrderItem> getOrderItems(Integer orderId) {
        return orderService.getOrderItems(orderId);
    }

    public List<OrderStatusLog> getStatusLogs(Integer orderId) {
        return orderService.getStatusLogs(orderId);
    }

    // ---- restock cepat ----
    public void addStock(Integer id, Integer qty) {
        if (qty != null && qty > 0) {
            menuMapper.addStock(id, qty);
        }
    }

    // ---- #5 sales report for this stall ----
    public SalesReport getSales(Integer stallId) {
        return orderService.salesByStall(stallId);
    }

    // ---- dashboard summary ----
    public int getPendingCount(Integer stallId) {
        return orderService.countOrdersByStallAndStatus(stallId, "PENDING");
    }

    public BigDecimal getTodayRevenue(Integer stallId) {
        return orderService.getTodayRevenueByStall(stallId);
    }

    public List<MenuItem> getLowStock(Integer stallId) {
        return menuMapper.findLowStock(stallId, 5);
    }

    // ---- dashboard charts ----
    // Revenue per day for this stall over the last 7 days (line chart).
    public List<DailySales> getDailyRevenue(Integer stallId) {
        return orderService.dailyRevenueByStall(stallId);
    }

    // How many of this stall's orders sit in each status (doughnut chart).
    // Returned in a fixed order so the template can pair them with labels.
    public List<Integer> getStatusCounts(Integer stallId) {
        List<Integer> counts = new java.util.ArrayList<>();
        counts.add(orderService.countOrdersByStallAndStatus(stallId, "PENDING"));
        counts.add(orderService.countOrdersByStallAndStatus(stallId, "PROCESSING"));
        counts.add(orderService.countOrdersByStallAndStatus(stallId, "READY"));
        counts.add(orderService.countOrdersByStallAndStatus(stallId, "PICKED_UP"));
        return counts;
    }

    // ---- reviews for this stall's menu ----
    public List<Review> getStallReviews(Integer stallId) {
        return reviewService.getReviewsByStall(stallId);
    }
}
