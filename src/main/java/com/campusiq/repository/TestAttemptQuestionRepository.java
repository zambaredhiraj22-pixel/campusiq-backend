package com.campusiq.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campusiq.entity.TestAttemptQuestion;

public interface TestAttemptQuestionRepository
        extends JpaRepository<
                TestAttemptQuestion,
                Long> {

    /*
     * Returns the questions assigned to an attempt
     * in their saved display order.
     */
    List<TestAttemptQuestion>
            findByTestAttempt_IdOrderByQuestionNumberAsc(
                    Long testAttemptId
            );

    /*
     * Finds an original Question Bank question
     * within this specific TestAttempt.
     */
    Optional<TestAttemptQuestion>
            findByTestAttempt_IdAndQuestionId(
                    Long testAttemptId,
                    Long questionId
            );
}