package com.canteen.entity;

import java.math.BigDecimal;

// One row of the "menu_items" table (a single food/drink item sold by a stall).
public class MenuItem {
    private Integer id;
    private Integer stallId;
    private String name;
    private String description;
    private BigDecimal price;
    private String imageUrl;
    private Boolean isAvailable;
    private Integer stock;

    public MenuItem() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getStallId() { return stallId; }
    public void setStallId(Integer stallId) { this.stallId = stallId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }

    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }

    // "orderable" when it is marked available AND still has stock.
    public boolean isOrderable() {
        return Boolean.TRUE.equals(isAvailable) && stock != null && stock > 0;
    }
}
