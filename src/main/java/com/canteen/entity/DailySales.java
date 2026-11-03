package com.canteen.entity;

import java.math.BigDecimal;

// One day of revenue, used by the dashboard charts (last 7 days).
public class DailySales {
    private String label;       // e.g. "2026-06-13"
    private BigDecimal revenue;

    public DailySales() {}

    public DailySales(String label, BigDecimal revenue) {
        this.label = label;
        this.revenue = revenue;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
}
