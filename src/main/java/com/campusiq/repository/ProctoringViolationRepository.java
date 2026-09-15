package com.campusiq.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.campusiq.entity.ProctoringViolation;
import com.campusiq.enums.ProctoringViolationType;

@Repository
public interface ProctoringViolationRepository
        extends JpaRepository<ProctoringViolation, Long> {

    List<ProctoringViolation> findByTestAttemptId(
            Long testAttemptId
    );

    long countByTestAttemptIdAndViolationType(
            Long testAttemptId,
            ProctoringViolationType violationType
    );
}