package com.ross.theovalguide.service;

import com.ross.theovalguide.model.CourseClass;
import com.ross.theovalguide.model.Professor;
import com.ross.theovalguide.model.Review;
import com.ross.theovalguide.repo.CourseClassRepository;
import com.ross.theovalguide.repo.ProfessorRepository;
import com.ross.theovalguide.repo.ReviewRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.ross.theovalguide.DTOS.review.CreateReviewRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviews;
    private final ProfessorRepository professors;
    private final CourseClassRepository classes;

    /* CRUD + Aggregates */

    /**
     * Creates a new review based on the provided request.
     * Handles validation, finding/creating the professor and class, and saving the
     * review.
     *
     * @param req the request containing review details
     * @return the saved review
     * @throws ResponseStatusException if validation fails or entities are not found
     */
    @Transactional
    public Review createReview(CreateReviewRequest req) {
        if (req.rating() == null || req.rating() < 1 || req.rating() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rating must be an integer 1..5");
        }
        if (req.difficulty() != null && (req.difficulty() < 1 || req.difficulty() > 5)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "difficulty must be 1..5 when provided");
        }
        if ((req.professorSlug() == null || req.professorSlug().isBlank())
                && (req.classCode() == null || req.classCode().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "professorSlug or classCode required");
        }

        var review = new Review();
        review.setRating(req.rating());
        review.setDifficulty(req.difficulty());
        review.setComment(req.comment());
        review.setUser(null);

        if (req.professorSlug() != null && !req.professorSlug().isBlank()) {
            var p = professors.findBySlugIgnoreCase(req.professorSlug())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "professor not found"));
            review.setProfessor(p);
        }

        if (req.classCode() != null && !req.classCode().isBlank()) {
            String classCode = req.classCode().trim();
            var c = classes.findByCodeIgnoreCase(classCode)
                    .or(() -> classes.findFirstByCodeLoose(classCode));

            CourseClass courseClass = c.orElseGet(() -> {
                if (!Boolean.TRUE.equals(req.createIfMissing())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "class not found");
                }

                String title = req.classTitle() == null ? "" : req.classTitle().trim();
                String department = req.department() == null ? "" : req.department().trim();
                String university = req.university() == null ? "" : req.university().trim();

                if (title.isBlank() || department.isBlank() || university.isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "classTitle, department, and university are required when createIfMissing is true");
                }

                var newClass = new CourseClass();
                newClass.setCode(classCode);
                newClass.setTitle(title);
                newClass.setDepartment(department);
                newClass.setUniversity(university);
                return classes.save(newClass);
            });

            review.setCourseClass(courseClass);
        }

        if (review.getProfessor() == null && review.getCourseClass() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "professorSlug or classCode required");
        }

        return createOrUpdate(review);
    }

    /**
     * Saves the review and recalculates aggregates for the associated professor and
     * class.
     *
     * @param review the review to save
     * @return the saved review
     */
    @Transactional
    public Review createOrUpdate(Review review) {
        Review saved = reviews.save(review);
        recalcAggregates(saved.getProfessor(), saved.getCourseClass());
        return saved;
    }

    private void recalcAggregates(Professor professor, CourseClass courseClass) {
        if (professor != null) {
            Double avg = reviews.avgRatingForProfessor(professor.getId());
            long cnt = reviews.countForProfessor(professor.getId());
            professor.setOverallRating(
                    avg == null ? null : BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
            professor.setTotalRatings((int) cnt);
            professors.save(professor);
        }

        if (courseClass != null) {
            Double avg = reviews.avgDifficultyForClass(courseClass.getId());
            long cnt = reviews.countForClass(courseClass.getId());
            courseClass.setDifficultyAvg(
                    avg == null ? null : BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
            courseClass.setTotalRatings((int) cnt);
            classes.save(courseClass);
        }
    }
}