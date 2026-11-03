package com.canteen.service;

import com.canteen.entity.CartItem;
import com.canteen.entity.CartView;
import com.canteen.entity.MenuItem;
import com.canteen.entity.Stall;
import com.canteen.mapper.CartMapper;
import com.canteen.mapper.MenuMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CartService {

    private final CartMapper cartMapper;
    private final MenuMapper menuMapper;

    public CartService(CartMapper cartMapper, MenuMapper menuMapper) {
        this.cartMapper = cartMapper;
        this.menuMapper = menuMapper;
    }

    // Add to cart. Returns an error message, or null on success.
    // Validate on the server so it can't be bypassed even if the button is hidden in the UI.
    public String addToCart(Integer userId, Integer menuItemId) {
        MenuItem item = menuMapper.findItemById(menuItemId);
        if (item == null || !item.isOrderable()) {
            return "This item is unavailable or out of stock.";
        }
        Stall stall = menuMapper.findStallById(item.getStallId());
        if (stall == null || !Boolean.TRUE.equals(stall.getIsOpen())) {
            return "The stall is currently closed, you can't order yet.";
        }

        CartItem existing = cartMapper.findByUserAndItem(userId, menuItemId);
        if (existing != null) {
            cartMapper.updateQuantity(existing.getId(), existing.getQuantity() + 1, userId);
        } else {
            CartItem c = new CartItem();
            c.setUserId(userId);
            c.setMenuItemId(menuItemId);
            c.setQuantity(1);
            cartMapper.insertCartItem(c);
        }
        return null;
    }

    public void updateQuantity(Integer userId, Integer id, Integer quantity) {
        if (quantity == null || quantity < 1) {
            cartMapper.deleteCartItem(id, userId);
        } else {
            cartMapper.updateQuantity(id, quantity, userId);
        }
    }

    public void removeItem(Integer userId, Integer id) {
        cartMapper.deleteCartItem(id, userId);
    }

    public List<CartView> getCartView(Integer userId) {
        return cartMapper.findCartView(userId);
    }

    public void clearCart(Integer userId) {
        cartMapper.deleteByUser(userId);
    }

    // subtotal = sum of (unitPrice * quantity), rounded to 2 decimals.
    public BigDecimal getSubtotal(Integer userId) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartView row : cartMapper.findCartView(userId)) {
            subtotal = subtotal.add(row.getLineTotal());
        }
        return subtotal.setScale(2, RoundingMode.HALF_UP);
    }
}
