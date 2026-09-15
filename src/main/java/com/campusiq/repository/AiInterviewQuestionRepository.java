package com.campusiq.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campusiq.entity.AiInterviewQuestion;
import com.campusiq.enums.AiInterviewQuestionType;

public interface AiInterviewQuestionRepository
        extends JpaRepository<AiInterviewQuestion, Long> {

    List<AiInterviewQuestion>
            findByInterviewSessionIdOrderByPoolOrderAsc(
                    Long interviewSessionId
            );

    List<AiInterviewQuestion>
            findByInterviewSessionIdAndQuestionTypeOrderByPoolOrderAsc(
                    Long interviewSessionId,
                    AiInterviewQuestionType questionType
            );

    List<AiInterviewQuestion>
            findByInterviewSessionIdAndSelectedForInterviewTrueOrderByInterviewOrderAsc(
                    Long interviewSessionId
            );

    long countByInterviewSessionId(
            Long interviewSessionId
    );

    long countByInterviewSessionIdAndSelectedForInterviewTrue(
            Long interviewSessionId
    );

    boolean existsByInterviewSessionIdAndPoolOrder(
            Long interviewSessionId,
            Integer poolOrder
    );

    void deleteByInterviewSessionId(
            Long interviewSessionId
    );
}