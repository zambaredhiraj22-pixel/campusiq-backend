package com.campusiq.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campusiq.entity.StudentProfile;
import com.campusiq.entity.User;

public interface StudentProfileRepository
        extends JpaRepository<StudentProfile, Long> {

    Optional<StudentProfile> findByUser(User user);

    boolean existsByUser(User user);

    Optional<StudentProfile> findByUserUsername(String username);

    List<StudentProfile>
            findByPlacementReadyTrueOrderByFullNameAsc();
}