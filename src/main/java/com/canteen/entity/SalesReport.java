package com.canteen.entity;

import java.math.BigDecimal;

// Sales summary result (used by the admin & superadmin reports).
public class SalesReport {
    private String stallName;     // null for the grand total
    private BigDecimal revenue;
    private Integer orderCount;

    public SalesReport() {}

    public String getStallName() { return stallName; }
    public void setStallName(String stallName) { this.stallName = stallName; }

    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }

    public Integer getOrderCount() { return orderCount; }
    public void setOrderCount(Integer orderCount) { this.orderCount = orderCount; }
}
