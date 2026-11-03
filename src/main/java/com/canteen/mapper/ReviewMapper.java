package com.canteen.mapper;

import com.canteen.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReviewMapper {

    int insertReview(Review review);

    List<Review> findByMenuItem(@Param("menuItemId") Integer menuItemId);

    // Average rating of a menu item (null if there are no reviews yet).
    Double averageRating(@Param("menuItemId") Integer menuItemId);

    // ---- vendor: reviews for all items in their stall ----
    List<Review> findByStall(@Param("stallId") Integer stallId);

    // ---- superadmin: moderate all reviews ----
    List<Review> findAllReviews();

    // Check whether the user has already reviewed this item.
    Review findByUserAndItem(@Param("userId") Integer userId, @Param("menuItemId") Integer menuItemId);

    // Update an existing review (if the user wants to change the rating/comment).
    int updateReview(@Param("userId") Integer userId, @Param("menuItemId") Integer menuItemId,
                     @Param("rating") Integer rating, @Param("comment") String comment);

    int deleteReview(@Param("id") Integer id);
}
