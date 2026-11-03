package com.canteen.mapper;

import com.canteen.entity.Discount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface DiscountMapper {

    Discount findActiveDiscount();

    // ---- M7: superadmin ----
    List<Discount> findAllDiscounts();

    int updateDiscount(@Param("id") Integer id, @Param("discountPercent") BigDecimal discountPercent);

    int setActive(@Param("id") Integer id, @Param("isActive") Boolean isActive);
}
