package com.canteen.mapper;

import com.canteen.entity.CartItem;
import com.canteen.entity.CartView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CartMapper {

    CartItem findByUserAndItem(@Param("userId") Integer userId, @Param("menuItemId") Integer menuItemId);

    int insertCartItem(CartItem c);

    int updateQuantity(@Param("id") Integer id, @Param("quantity") Integer quantity, @Param("userId") Integer userId);

    int deleteCartItem(@Param("id") Integer id, @Param("userId") Integer userId);

    int deleteByUser(@Param("userId") Integer userId);

    List<CartView> findCartView(@Param("userId") Integer userId);
}
