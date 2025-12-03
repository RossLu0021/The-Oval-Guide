package com.ross.theovalguide.controllers;

import com.ross.theovalguide.DTOS.review.CreateReviewRequest;
import com.ross.theovalguide.model.Review;
import com.ross.theovalguide.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    private ReviewController controller;

    @BeforeEach
    void setUp() {
        controller = new ReviewController(reviewService);
    }

    @Test
    void createDelegatesToService() {
        Review saved = new Review();
        saved.setId(UUID.randomUUID());
        when(reviewService.createReview(any(CreateReviewRequest.class))).thenReturn(saved);

        CreateReviewRequest request = new CreateReviewRequest(
                5,
                3,
                "Great class",
                List.of(),
                null,
                "CS 1234",
                null,
                null,
                null,
                null);

        ResponseEntity<?> response = controller.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(reviewService).createReview(request);
    }
}
