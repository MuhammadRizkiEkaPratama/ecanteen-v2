package com.canteen.entity;

import java.math.BigDecimal;

// Joined view of one cart row + its menu item + its stall.
// Used only for displaying the cart and computing discounts.
public class CartView {
    private Integer id;            // cart_items.id
    private Integer menuItemId;
    private String itemName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private Integer stallId;
    private String stallName;
    private BigDecimal stallDiscountPercent;

    public CartView() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getMenuItemId() { return menuItemId; }
    public void setMenuItemId(Integer menuItemId) { this.menuItemId = menuItemId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Integer getStallId() { return stallId; }
    public void setStallId(Integer stallId) { this.stallId = stallId; }

    public String getStallName() { return stallName; }
    public void setStallName(String stallName) { this.stallName = stallName; }

    public BigDecimal getStallDiscountPercent() { return stallDiscountPercent; }
    public void setStallDiscountPercent(BigDecimal stallDiscountPercent) { this.stallDiscountPercent = stallDiscountPercent; }

    // line total = unitPrice * quantity (handy for the template)
    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
