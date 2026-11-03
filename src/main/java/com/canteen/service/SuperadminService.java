package com.canteen.service;

import com.canteen.entity.DailySales;
import com.canteen.entity.Discount;
import com.canteen.entity.Order;
import com.canteen.entity.Review;
import com.canteen.entity.SalesReport;
import com.canteen.entity.Stall;
import com.canteen.entity.User;
import com.canteen.mapper.DiscountMapper;
import com.canteen.mapper.MenuMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SuperadminService {

    private final DiscountMapper discountMapper;
    private final MenuMapper menuMapper;
    private final OrderService orderService;
    private final UserService userService;
    private final ReviewService reviewService;

    public SuperadminService(DiscountMapper discountMapper, MenuMapper menuMapper, OrderService orderService,
                             UserService userService, ReviewService reviewService) {
        this.discountMapper = discountMapper;
        this.menuMapper = menuMapper;
        this.orderService = orderService;
        this.userService = userService;
        this.reviewService = reviewService;
    }

    // ---- global admin discounts ----
    public List<Discount> getAllDiscounts() {
        return discountMapper.findAllDiscounts();
    }

    public void updateDiscount(Integer id, BigDecimal discountPercent) {
        discountMapper.updateDiscount(id, discountPercent);
    }

    public void setActive(Integer id, Boolean isActive) {
        discountMapper.setActive(id, isActive);
    }

    // ---- per-stall discounts ----
    public List<Stall> getAllStalls() {
        return menuMapper.findAllStalls();
    }

    public void updateStallDiscount(Integer id, BigDecimal discountPercent) {
        menuMapper.updateStallDiscount(id, discountPercent);
    }

    // ---- #5 sales report for the whole canteen ----
    public SalesReport getSalesTotal() {
        return orderService.salesTotal();
    }

    public List<SalesReport> getSalesPerStall() {
        return orderService.salesPerStall();
    }

    // ---- manage users ----
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    public void setUserActive(Integer id, boolean active) {
        userService.setActive(id, active);
    }

    public boolean resetUserPassword(Integer id, String newPassword) {
        return userService.resetPassword(id, newPassword);
    }

    // ---- suspend / activate stall ----
    public void setStallActive(Integer id, boolean active) {
        menuMapper.setStallActive(id, active);
    }

    // ---- all canteen transactions ----
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    public Order getOrder(Integer id) {
        return orderService.getOrder(id);
    }

    public List<com.canteen.entity.OrderItem> getOrderItems(Integer id) {
        return orderService.getOrderItems(id);
    }

    // ---- review moderation ----
    public List<Review> getAllReviews() {
        return reviewService.getAllReviews();
    }

    public void deleteReview(Integer id) {
        reviewService.deleteReview(id);
    }

    // ---- dashboard statistics ----
    public int getTotalUsers() {
        return userService.getAllUsers().size();
    }

    // number of stalls still active (vendors approved and not suspended)
    public long getTotalActiveStalls() {
        return menuMapper.findAllStalls().stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .count();
    }

    public BigDecimal getTotalRevenue() {
        SalesReport total = orderService.salesTotal();
        return total != null ? total.getRevenue() : BigDecimal.ZERO;
    }

    public int getOrdersToday() {
        return orderService.countOrdersToday();
    }

    // ---- dashboard charts ----
    // Revenue per day across the whole canteen, last 7 days (line chart).
    public List<DailySales> getDailyRevenue() {
        return orderService.dailyRevenueAll();
    }
}
