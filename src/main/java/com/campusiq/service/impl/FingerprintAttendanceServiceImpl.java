package com.campusiq.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.campusiq.dto.AttendanceMarkRequest;
import com.campusiq.dto.AttendanceMarkResponse;
import com.campusiq.dto.FingerprintEnrollmentRequest;
import com.campusiq.dto.FingerprintEnrollmentResponse;
import com.campusiq.entity.AttendanceRecord;
import com.campusiq.entity.AttendanceSession;
import com.campusiq.entity.FingerprintEnrollment;
import com.campusiq.entity.StudentProfile;
import com.campusiq.exception.StudentProfileNotFoundException;
import com.campusiq.repository.AttendanceRecordRepository;
import com.campusiq.repository.AttendanceSessionRepository;
import com.campusiq.repository.FingerprintEnrollmentRepository;
import com.campusiq.repository.StudentProfileRepository;
import com.campusiq.service.FingerprintAttendanceService;

@Service
public class FingerprintAttendanceServiceImpl
        implements FingerprintAttendanceService {

    private final FingerprintEnrollmentRepository fingerprintEnrollmentRepository;

    private final AttendanceRecordRepository attendanceRecordRepository;

    private final AttendanceSessionRepository attendanceSessionRepository;

    private final StudentProfileRepository studentProfileRepository;

    public FingerprintAttendanceServiceImpl(
            FingerprintEnrollmentRepository fingerprintEnrollmentRepository,
            AttendanceRecordRepository attendanceRecordRepository,
            AttendanceSessionRepository attendanceSessionRepository,
            StudentProfileRepository studentProfileRepository) {

        this.fingerprintEnrollmentRepository = fingerprintEnrollmentRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.studentProfileRepository = studentProfileRepository;
    }

    @Override
    @Transactional
    public FingerprintEnrollmentResponse enrollFingerprint(
            FingerprintEnrollmentRequest request) {

        validateEnrollmentRequest(request);

        StudentProfile studentProfile = studentProfileRepository
                .findById(request.getStudentProfileId())
                .orElseThrow(() ->
                        new StudentProfileNotFoundException(
                                "Student profile not found with id: "
                                        + request.getStudentProfileId()
                        )
                );

        FingerprintEnrollment existingEnrollment =
                fingerprintEnrollmentRepository
                        .findByDeviceIdAndFingerprintTemplateId(
                                request.getDeviceId(),
                                request.getFingerprintTemplateId()
                        )
                        .orElse(null);

        if (existingEnrollment != null) {

            if (existingEnrollment.getStudentProfile()
                    .getId()
                    .equals(studentProfile.getId())) {

                return createEnrollmentResponse(
                        existingEnrollment,
                        "Fingerprint is already enrolled for this student"
                );
            }

            throw new IllegalStateException(
                    "This fingerprint is already linked to another student"
            );
        }

        FingerprintEnrollment enrollment =
                new FingerprintEnrollment();

        enrollment.setDeviceId(request.getDeviceId());
        enrollment.setFingerprintTemplateId(
                request.getFingerprintTemplateId()
        );
        enrollment.setStudentProfile(studentProfile);
        enrollment.setActive(true);

        FingerprintEnrollment savedEnrollment =
                fingerprintEnrollmentRepository.save(enrollment);

        return createEnrollmentResponse(
                savedEnrollment,
                "Fingerprint enrolled successfully"
        );
    }

    @Override
    @Transactional
    public AttendanceMarkResponse markAttendance(
            AttendanceMarkRequest request) {

        validateAttendanceRequest(request);

        FingerprintEnrollment enrollment =
                fingerprintEnrollmentRepository
                        .findByDeviceIdAndFingerprintTemplateId(
                                request.getDeviceId(),
                                request.getFingerprintTemplateId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Fingerprint is not registered"
                                )
                        );

        if (!Boolean.TRUE.equals(enrollment.getActive())) {

            throw new IllegalStateException(
                    "Fingerprint enrollment is inactive"
            );
        }

        StudentProfile studentProfile =
                enrollment.getStudentProfile();

        LocalDate today = LocalDate.now();

        AttendanceSession session =
                attendanceSessionRepository
                        .findBySessionDate(today)
                        .orElse(null);

        if (session == null) {

            session = new AttendanceSession();

            session.setSessionDate(today);
            session.setDeviceId(request.getDeviceId());
            session.setActive(true);

            attendanceSessionRepository.save(session);

            recalculateAllAttendancePercentages();
        }

        AttendanceRecord existingAttendance =
                attendanceRecordRepository
                        .findByStudentProfileAndAttendanceDate(
                                studentProfile,
                                today
                        )
                        .orElse(null);

        if (existingAttendance != null) {

            return createAttendanceResponse(
                    existingAttendance,
                    "Attendance already marked today"
            );
        }

        AttendanceRecord attendanceRecord =
                new AttendanceRecord();

        attendanceRecord.setStudentProfile(studentProfile);
        attendanceRecord.setAttendanceDate(today);
        attendanceRecord.setStatus("PRESENT");
        attendanceRecord.setDeviceId(request.getDeviceId());
        attendanceRecord.setFingerprintTemplateId(
                request.getFingerprintTemplateId()
        );

        AttendanceRecord savedAttendance =
                attendanceRecordRepository.save(attendanceRecord);

        updateStudentAttendancePercentage(studentProfile);

        return createAttendanceResponse(
                savedAttendance,
                "Attendance marked successfully"
        );
    }

    private void updateStudentAttendancePercentage(
            StudentProfile studentProfile) {

        long totalSessions =
                attendanceSessionRepository.count();

        if (totalSessions == 0) {

            studentProfile.setAttendancePercentage(0.0);
            studentProfileRepository.save(studentProfile);

            return;
        }

        long presentDays =
                attendanceRecordRepository
                        .countByStudentProfile(studentProfile);

        double attendancePercentage =
                ((double) presentDays / totalSessions) * 100;

        attendancePercentage =
                Math.round(attendancePercentage * 100.0) / 100.0;

        studentProfile.setAttendancePercentage(
                attendancePercentage
        );

        studentProfileRepository.save(studentProfile);
    }

    private void recalculateAllAttendancePercentages() {

        List<StudentProfile> studentProfiles =
                studentProfileRepository.findAll();

        for (StudentProfile studentProfile : studentProfiles) {

            updateStudentAttendancePercentage(
                    studentProfile
            );
        }
    }

    private FingerprintEnrollmentResponse createEnrollmentResponse(
            FingerprintEnrollment enrollment,
            String message) {

        StudentProfile studentProfile =
                enrollment.getStudentProfile();

        return new FingerprintEnrollmentResponse(
                enrollment.getId(),
                studentProfile.getId(),
                studentProfile.getFullName(),
                enrollment.getDeviceId(),
                enrollment.getFingerprintTemplateId(),
                message
        );
    }

    private AttendanceMarkResponse createAttendanceResponse(
            AttendanceRecord attendanceRecord,
            String message) {

        StudentProfile studentProfile =
                attendanceRecord.getStudentProfile();

        return new AttendanceMarkResponse(
                attendanceRecord.getId(),
                studentProfile.getId(),
                studentProfile.getFullName(),
                attendanceRecord.getAttendanceDate(),
                attendanceRecord.getMarkedAt(),
                attendanceRecord.getStatus(),
                message
        );
    }

    private void validateEnrollmentRequest(
            FingerprintEnrollmentRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Enrollment request cannot be null"
            );
        }

        if (request.getStudentProfileId() == null) {
            throw new IllegalArgumentException(
                    "Student profile id is required"
            );
        }

        validateDeviceAndFingerprint(
                request.getDeviceId(),
                request.getFingerprintTemplateId()
        );
    }

    private void validateAttendanceRequest(
            AttendanceMarkRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Attendance request cannot be null"
            );
        }

        validateDeviceAndFingerprint(
                request.getDeviceId(),
                request.getFingerprintTemplateId()
        );
    }

    private void validateDeviceAndFingerprint(
            String deviceId,
            Integer fingerprintTemplateId) {

        if (deviceId == null || deviceId.isBlank()) {

            throw new IllegalArgumentException(
                    "Device id is required"
            );
        }

        if (fingerprintTemplateId == null
                || fingerprintTemplateId < 0) {

            throw new IllegalArgumentException(
                    "Valid fingerprint template id is required"
            );
        }
    }
}