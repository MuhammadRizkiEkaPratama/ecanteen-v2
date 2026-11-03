package com.canteen.entity;

import java.math.BigDecimal;

// One row of the "discounts" table (a global admin discount; the active one applies to every order).
public class Discount {
    private Integer id;
    private String name;
    private BigDecimal discountPercent;
    private Boolean isActive;

    public Discount() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
