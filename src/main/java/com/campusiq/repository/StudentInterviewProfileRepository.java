package com.campusiq.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campusiq.entity.StudentInterviewProfile;
import com.campusiq.entity.StudentProfile;

public interface StudentInterviewProfileRepository
        extends JpaRepository<StudentInterviewProfile, Long> {

    Optional<StudentInterviewProfile>
            findByStudentProfile(StudentProfile studentProfile);

    Optional<StudentInterviewProfile>
            findByStudentProfileId(Long studentProfileId);

    boolean existsByStudentProfileId(Long studentProfileId);
}