package com.canteen.service;

import com.canteen.entity.Review;
import com.canteen.mapper.OrderMapper;
import com.canteen.mapper.ReviewMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;

    public ReviewService(ReviewMapper reviewMapper, OrderMapper orderMapper) {
        this.reviewMapper = reviewMapper;
        this.orderMapper = orderMapper;
    }

    // Returns an error message, or null on success.
    public String addReview(Integer menuItemId, Integer userId, Integer rating, String comment) {
        if (rating == null || rating < 1) rating = 1;
        if (rating > 5) rating = 5;

        // the user must have ordered this item and the order must be READY/PICKED_UP
        if (orderMapper.countCompletedOrdersWithItem(userId, menuItemId) == 0) {
            return "You haven't ordered this item yet, or the order hasn't been completed.";
        }

        // if a review already exists, just update it (don't create a duplicate)
        Review existing = reviewMapper.findByUserAndItem(userId, menuItemId);
        if (existing != null) {
            reviewMapper.updateReview(userId, menuItemId, rating, comment);
            return null;
        }

        Review review = new Review();
        review.setMenuItemId(menuItemId);
        review.setUserId(userId);
        review.setRating(rating);
        review.setComment(comment);
        reviewMapper.insertReview(review);
        return null;
    }

    public List<Review> getReviews(Integer menuItemId) {
        return reviewMapper.findByMenuItem(menuItemId);
    }

    // vendor: reviews for all items in their stall
    public List<Review> getReviewsByStall(Integer stallId) {
        return reviewMapper.findByStall(stallId);
    }

    // superadmin: all reviews (moderation)
    public List<Review> getAllReviews() {
        return reviewMapper.findAllReviews();
    }

    public void deleteReview(Integer id) {
        reviewMapper.deleteReview(id);
    }

    // Average rating, rounded to 1 decimal; 0 if there are no reviews yet.
    public double getAverage(Integer menuItemId) {
        Double avg = reviewMapper.averageRating(menuItemId);
        if (avg == null) {
            return 0.0;
        }
        return Math.round(avg * 10.0) / 10.0;
    }
}
