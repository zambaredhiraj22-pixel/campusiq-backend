package com.campusiq.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campusiq.entity.AiInterviewAnswer;

public interface AiInterviewAnswerRepository
        extends JpaRepository<AiInterviewAnswer, Long> {

    Optional<AiInterviewAnswer>
            findByInterviewSessionIdAndInterviewQuestionId(
                    Long interviewSessionId,
                    Long interviewQuestionId
            );

    List<AiInterviewAnswer>
            findByInterviewSessionIdOrderByInterviewQuestionInterviewOrderAsc(
                    Long interviewSessionId
            );

    long countByInterviewSessionId(
            Long interviewSessionId
    );

    long countByInterviewSessionIdAndEvaluatedTrue(
            Long interviewSessionId
    );

    boolean existsByInterviewSessionIdAndInterviewQuestionId(
            Long interviewSessionId,
            Long interviewQuestionId
    );

    void deleteByInterviewSessionId(
            Long interviewSessionId
    );
}