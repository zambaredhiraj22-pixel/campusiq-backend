package com.campusiq.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.campusiq.entity.MockTestQuestion;

public interface MockTestQuestionRepository
        extends JpaRepository<MockTestQuestion, Long> {

    /*
     * Returns all mapped questions of a mock test
     * in the exact order defined by questionOrder.
     *
     * EntityGraph also loads Question data immediately,
     * which is useful when starting the test.
     */
    @EntityGraph(attributePaths = "question")
    List<MockTestQuestion>
            findByMockTestIdOrderByQuestionOrderAsc(
                    Long mockTestId
            );

    /*
     * Checks whether this mock test has
     * fixed question mappings.
     *
     * AI personalized test:
     * true  -> use exact mapped questions.
     *
     * Existing manual test:
     * false -> existing random/manual logic can continue.
     */
    boolean existsByMockTestId(
            Long mockTestId
    );

    /*
     * Used to verify that an AI personalized
     * mock test contains exactly 60 questions.
     */
    long countByMockTestId(
            Long mockTestId
    );

    /*
     * Prevents/checks duplicate question mapping
     * for the same mock test.
     */
    boolean existsByMockTestIdAndQuestionId(
            Long mockTestId,
            Long questionId
    );

    /*
     * Useful if creation fails or faculty
     * recreates a personalized test before use.
     */
    void deleteByMockTestId(
            Long mockTestId
    );
}