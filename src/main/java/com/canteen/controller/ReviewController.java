package com.canteen.controller;

import com.canteen.entity.User;
import com.canteen.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/review/add")
    public String addReview(@RequestParam("menuItemId") Integer menuItemId,
                            @RequestParam("rating") Integer rating,
                            @RequestParam(value = "comment", required = false) String comment,
                            HttpSession session, RedirectAttributes ra) {
        User user = (User) session.getAttribute("loginUser");
        String err = reviewService.addReview(menuItemId, user.getId(), rating, comment);
        if (err != null) {
            ra.addFlashAttribute("reviewError", err);
        } else {
            ra.addFlashAttribute("reviewMessage", "Your review has been saved!");
        }
        return "redirect:/menu/item/" + menuItemId;
    }
}
