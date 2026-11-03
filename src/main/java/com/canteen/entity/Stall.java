package com.canteen.entity;

import java.math.BigDecimal;

// One row of the "stalls" table (a canteen stall owned by an admin/vendor).
public class Stall {
    private Integer id;
    private String name;
    private String description;
    private Boolean isActive;
    private Boolean isOpen;
    private BigDecimal discountPercent;

    public Stall() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Boolean getIsOpen() { return isOpen; }
    public void setIsOpen(Boolean isOpen) { this.isOpen = isOpen; }

    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
}
