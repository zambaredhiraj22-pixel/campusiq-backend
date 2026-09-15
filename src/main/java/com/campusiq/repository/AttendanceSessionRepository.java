package com.campusiq.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.campusiq.entity.AttendanceSession;

@Repository
public interface AttendanceSessionRepository
        extends JpaRepository<AttendanceSession, Long> {

    Optional<AttendanceSession> findBySessionDate(
            LocalDate sessionDate
    );

    boolean existsBySessionDate(
            LocalDate sessionDate
    );

    long countByActiveTrue();
}