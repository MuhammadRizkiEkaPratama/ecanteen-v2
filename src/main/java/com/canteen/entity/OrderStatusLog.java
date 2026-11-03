package com.canteen.entity;

import java.time.LocalDateTime;

// One row of the "order_status_logs" table (a timestamped history entry for an order's status changes).
public class OrderStatusLog {
    private Integer id;
    private Integer orderId;
    private String status;
    private LocalDateTime changedAt;

    public OrderStatusLog() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}
