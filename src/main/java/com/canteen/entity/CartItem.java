package com.canteen.entity;

// One row of the "cart_items" table (one menu item + quantity in a user's cart).
public class CartItem {
    private Integer id;
    private Integer userId;
    private Integer menuItemId;
    private Integer quantity;

    public CartItem() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getMenuItemId() { return menuItemId; }
    public void setMenuItemId(Integer menuItemId) { this.menuItemId = menuItemId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
