package com.campusiq.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusiq.dto.AttendanceMarkRequest;
import com.campusiq.dto.AttendanceMarkResponse;
import com.campusiq.dto.FingerprintEnrollmentRequest;
import com.campusiq.dto.FingerprintEnrollmentResponse;
import com.campusiq.hardware.SerialAttendanceListener;
import com.campusiq.service.FingerprintAttendanceService;

@RestController
@RequestMapping("/api/fingerprint")
public class FingerprintAttendanceController {

    private final FingerprintAttendanceService
            fingerprintAttendanceService;

    private final SerialAttendanceListener
            serialAttendanceListener;

    public FingerprintAttendanceController(
            FingerprintAttendanceService fingerprintAttendanceService,
            SerialAttendanceListener serialAttendanceListener) {

        this.fingerprintAttendanceService =
                fingerprintAttendanceService;

        this.serialAttendanceListener =
                serialAttendanceListener;
    }

    @PostMapping("/enroll")
    public ResponseEntity<FingerprintEnrollmentResponse>
            enrollFingerprint(
                    @RequestBody FingerprintEnrollmentRequest request) {

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

        if (request.getDeviceId() == null
                || request.getDeviceId().isBlank()) {

            throw new IllegalArgumentException(
                    "Device id is required"
            );
        }

        if (request.getFingerprintTemplateId() == null
                || request.getFingerprintTemplateId() <= 0) {

            throw new IllegalArgumentException(
                    "Valid fingerprint template id is required"
            );
        }

        // ---------------------------------------------
        // STEP 1:
        // Physically enroll fingerprint in R307S
        // through ESP32 using USB Serial.
        // ---------------------------------------------

        boolean hardwareEnrollmentSuccessful =
                serialAttendanceListener
                        .enrollFingerprintOnDevice(
                                request.getFingerprintTemplateId()
                        );

        if (!hardwareEnrollmentSuccessful) {

            throw new IllegalStateException(
                    "Fingerprint could not be enrolled on ESP32 device"
            );
        }

        // ---------------------------------------------
        // STEP 2:
        // After hardware enrollment succeeds,
        // save StudentProfile ↔ Template ID mapping
        // using the existing service logic.
        // ---------------------------------------------

        FingerprintEnrollmentResponse response =
                fingerprintAttendanceService
                        .enrollFingerprint(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/attendance")
    public ResponseEntity<AttendanceMarkResponse>
            markAttendance(
                    @RequestBody AttendanceMarkRequest request) {

        AttendanceMarkResponse response =
                fingerprintAttendanceService
                        .markAttendance(request);

        return ResponseEntity.ok(response);
    }
}