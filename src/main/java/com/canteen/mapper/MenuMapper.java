package com.canteen.mapper;

import com.canteen.entity.MenuItem;
import com.canteen.entity.Stall;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface MenuMapper {

    // ---- M2: browse ----
    List<Stall> findActiveStalls();

    List<MenuItem> findItemsByStall(@Param("stallId") Integer stallId);

    List<MenuItem> searchItemsByStall(@Param("stallId") Integer stallId, @Param("keyword") String keyword);

    MenuItem findItemById(@Param("id") Integer id);

    Stall findStallById(@Param("id") Integer id);

    // ---- M6: admin/vendor menu CRUD ----
    int insertItem(MenuItem item);

    int updateItem(MenuItem item);

    int deleteItem(@Param("id") Integer id);

    int toggleAvailability(@Param("id") Integer id, @Param("isAvailable") Boolean isAvailable);

    int decreaseStock(@Param("id") Integer id, @Param("qty") Integer qty);

    int restoreStock(@Param("id") Integer id, @Param("qty") Integer qty);

    // Add stock (quick restock by the admin, without editing the full item).
    int addStock(@Param("id") Integer id, @Param("qty") Integer qty);

    int toggleStallOpen(@Param("id") Integer id, @Param("isOpen") Boolean isOpen);

    // items with low stock (<= threshold) in a single stall
    List<MenuItem> findLowStock(@Param("stallId") Integer stallId, @Param("threshold") Integer threshold);

    // superadmin: activate/deactivate stall
    int setStallActive(@Param("id") Integer id, @Param("isActive") Boolean isActive);

    // ---- M7: superadmin ----
    List<Stall> findAllStalls();

    int updateStallDiscount(@Param("id") Integer id, @Param("discountPercent") BigDecimal discountPercent);

    // ---- vendor approval: create the stall when a vendor is approved ----
    int insertStall(Stall stall);
}
