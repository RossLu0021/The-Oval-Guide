package com.ross.theovalguide.controllers;

import com.ross.theovalguide.DTOS.review.CreateReviewRequest;
import com.ross.theovalguide.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * REST controller for managing reviews.
 * Delegates business logic to {@link ReviewService}.
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateReviewRequest req) {
        var saved = reviewService.createReview(req);

        return ResponseEntity
                .created(URI.create("/api/reviews/" + saved.getId()))
                .body(new IdResponse(saved.getId().toString()));
    }

    public record IdResponse(String id) {
    }
}