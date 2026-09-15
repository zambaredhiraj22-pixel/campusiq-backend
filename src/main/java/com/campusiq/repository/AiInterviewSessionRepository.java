package com.campusiq.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campusiq.entity.AiInterviewSession;
import com.campusiq.enums.AiInterviewStatus;

public interface AiInterviewSessionRepository
        extends JpaRepository<AiInterviewSession, Long> {

    Optional<AiInterviewSession>
            findByMockTestResultId(Long mockTestResultId);

    boolean existsByMockTestResultId(Long mockTestResultId);

    List<AiInterviewSession>
            findByStudentProfileIdOrderByCreatedAtDesc(
                    Long studentProfileId
            );

    Optional<AiInterviewSession>
            findFirstByStudentProfileIdOrderByCreatedAtDesc(
                    Long studentProfileId
            );

    List<AiInterviewSession>
            findByStudentProfileIdAndStatusOrderByCreatedAtDesc(
                    Long studentProfileId,
                    AiInterviewStatus status
            );

    List<AiInterviewSession>
            findByStatus(AiInterviewStatus status);
}