package com.snoozeshare.controller.guest;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.snoozeshare.api.request.SubmitReviewRequest;
import com.snoozeshare.api.response.ReviewResponse;
import com.snoozeshare.domain.model.Booking;
import com.snoozeshare.repository.BookingRepository;
import com.snoozeshare.service.ReviewService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
public class GuestReviewController {

    private final ReviewService reviewService;
    private final BookingRepository bookings;

    public GuestReviewController(ReviewService reviewService,
                                 BookingRepository bookings) {
        this.reviewService = reviewService;
        this.bookings = bookings;
    }

    @PostMapping("/api/guest/bookings/{id}/review")
    ReviewResponse submitReview(@PathVariable UUID id,
                                @Valid @RequestBody SubmitReviewRequest req,
                                HttpServletRequest request) {
        UUID guestId = ResolvedUser.id(request);
        Booking booking = bookings.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Booking does not exist"));
        if (!booking.getGuestId().equals(guestId)) {
            throw new ForbiddenException(
                    "You do not have access to this booking");
        }
        return ReviewResponse.from(
                reviewService.submit(id, guestId, req.rating(), req.comment()));
    }
}
