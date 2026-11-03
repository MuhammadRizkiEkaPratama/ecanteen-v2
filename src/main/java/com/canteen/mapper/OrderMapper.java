package com.canteen.mapper;

import com.canteen.entity.DailySales;
import com.canteen.entity.Order;
import com.canteen.entity.OrderItem;
import com.canteen.entity.OrderStatusLog;
import com.canteen.entity.SalesReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper {

    int insertOrder(Order order);

    int insertOrderItem(OrderItem item);

    List<Order> findOrdersByUser(@Param("userId") Integer userId);

    List<OrderItem> findItemsByOrder(@Param("orderId") Integer orderId);

    int updateStatus(@Param("id") Integer id, @Param("status") String status);

    // Tag all orders from one checkout with the same group id (for history grouping).
    int updateOrderGroup(@Param("id") Integer id, @Param("groupId") Integer groupId);

    Order findOrderById(@Param("id") Integer id);

    // ---- M6: admin sees all incoming orders ----
    List<Order> findAllOrders();

    // ---- orders that contain at least one item from a stall (for the vendor) ----
    // status may be null = all statuses.
    List<Order> findOrdersByStall(@Param("stallId") Integer stallId, @Param("status") String status);

    // how many items of this order belong to that stall (0 = not this stall's order)
    int countOrderInStall(@Param("orderId") Integer orderId, @Param("stallId") Integer stallId);

    // number of this stall's orders with a given status (for the dashboard summary)
    int countOrdersByStallAndStatus(@Param("stallId") Integer stallId, @Param("status") String status);

    // this stall's revenue today (from items sold today)
    java.math.BigDecimal todayRevenueByStall(@Param("stallId") Integer stallId);

    // ---- superadmin: statistics ----
    int countOrdersToday();

    // ---- #11 audit: order status history ----
    int insertStatusLog(OrderStatusLog log);

    List<OrderStatusLog> findStatusLogsByOrder(@Param("orderId") Integer orderId);

    // ---- cancel & customer notification ----
    int countReadyOrders(@Param("userId") Integer userId);

    // Check whether the user has ordered this item and it is READY/PICKED_UP.
    int countCompletedOrdersWithItem(@Param("userId") Integer userId,
                                     @Param("menuItemId") Integer menuItemId);

    // User's order history with an optional status filter (null = all).
    List<Order> findOrdersByUserFiltered(@Param("userId") Integer userId,
                                         @Param("status") String status);

    // ---- #5 sales report ----
    SalesReport salesByStall(@Param("stallId") Integer stallId);

    List<SalesReport> salesPerStall();

    SalesReport salesTotal();

    // ---- dashboard charts: revenue grouped by day (last 7 days) ----
    List<DailySales> dailyRevenueByStall(@Param("stallId") Integer stallId);

    List<DailySales> dailyRevenueAll();
}
