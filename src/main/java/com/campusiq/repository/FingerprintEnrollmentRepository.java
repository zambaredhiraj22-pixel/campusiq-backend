package com.campusiq.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.campusiq.entity.FingerprintEnrollment;

@Repository
public interface FingerprintEnrollmentRepository
        extends JpaRepository<FingerprintEnrollment, Long> {

    Optional<FingerprintEnrollment>
        findByDeviceIdAndFingerprintTemplateId(
            String deviceId,
            Integer fingerprintTemplateId
        );

    boolean existsByDeviceIdAndFingerprintTemplateId(
        String deviceId,
        Integer fingerprintTemplateId
    );
}