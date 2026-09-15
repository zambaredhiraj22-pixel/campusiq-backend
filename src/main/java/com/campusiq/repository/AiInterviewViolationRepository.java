package com.campusiq.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.campusiq.entity.AiInterviewViolation;
import com.campusiq.enums.ProctoringViolationType;

@Repository
public interface AiInterviewViolationRepository
        extends JpaRepository<AiInterviewViolation, Long> {

    List<AiInterviewViolation>
            findByInterviewSessionIdOrderByDetectedAtAsc(
                    Long interviewSessionId
            );

    long countByInterviewSessionIdAndViolationType(
            Long interviewSessionId,
            ProctoringViolationType violationType
    );
}