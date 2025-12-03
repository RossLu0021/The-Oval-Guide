package com.ross.theovalguide.service;

import com.ross.theovalguide.DTOS.review.CreateReviewRequest;
import com.ross.theovalguide.model.CourseClass;
import com.ross.theovalguide.model.Review;
import com.ross.theovalguide.repo.CourseClassRepository;
import com.ross.theovalguide.repo.ProfessorRepository;
import com.ross.theovalguide.repo.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviews;
    @Mock
    private ProfessorRepository professors;
    @Mock
    private CourseClassRepository classes;

    private ReviewService service;

    @BeforeEach
    void setUp() {
        service = new ReviewService(reviews, professors, classes);
    }

    @Test
    void createReviewWithExistingClassUsesExistingRecord() {
        CourseClass existing = new CourseClass();
        existing.setId(UUID.randomUUID());
        when(classes.findByCodeIgnoreCase("CS 1234")).thenReturn(Optional.of(existing));

        Review persisted = new Review();
        persisted.setId(UUID.randomUUID());
        when(reviews.save(any(Review.class))).thenReturn(persisted);

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

        Review result = service.createReview(request);

        assertThat(result).isSameAs(persisted);
        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
        verify(reviews).save(reviewCaptor.capture());
        assertSame(existing, reviewCaptor.getValue().getCourseClass());
        verify(classes, never()).save(any(CourseClass.class));
    }

    @Test
    void createReviewCreatesCourseClassWhenMissingAndRequested() {
        when(classes.findByCodeIgnoreCase("CS 2201")).thenReturn(Optional.empty());
        when(classes.findFirstByCodeLoose("CS 2201")).thenReturn(Optional.empty());

        CourseClass stored = new CourseClass();
        stored.setId(UUID.randomUUID());
        when(classes.save(any(CourseClass.class))).thenAnswer(invocation -> {
            CourseClass created = invocation.getArgument(0);
            assertThat(created.getCode()).isEqualTo("CS 2201");
            return stored;
        });

        Review persisted = new Review();
        persisted.setId(UUID.randomUUID());
        when(reviews.save(any(Review.class))).thenReturn(persisted);

        CreateReviewRequest request = new CreateReviewRequest(
                4,
                2,
                "Challenging but fair",
                List.of(),
                null,
                "CS 2201",
                true,
                "Intro to Programming",
                "Computer Science",
                "Vanderbilt");

        Review result = service.createReview(request);

        assertThat(result).isSameAs(persisted);
        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
        verify(reviews).save(reviewCaptor.capture());
        assertSame(stored, reviewCaptor.getValue().getCourseClass());
        verify(classes).save(any(CourseClass.class));
    }

    @Test
    void createReviewThrowsWhenRatingInvalid() {
        CreateReviewRequest request = new CreateReviewRequest(
                6, // Invalid
                3,
                "Great class",
                List.of(),
                null,
                "CS 1234",
                null,
                null,
                null,
                null);

        assertThatThrownBy(() -> service.createReview(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("rating must be an integer 1..5");
    }
}
