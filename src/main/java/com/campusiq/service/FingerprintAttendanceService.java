package com.campusiq.service;

import com.campusiq.dto.AttendanceMarkRequest;
import com.campusiq.dto.AttendanceMarkResponse;
import com.campusiq.dto.FingerprintEnrollmentRequest;
import com.campusiq.dto.FingerprintEnrollmentResponse;

public interface FingerprintAttendanceService {

    FingerprintEnrollmentResponse enrollFingerprint(
            FingerprintEnrollmentRequest request
    );

    AttendanceMarkResponse markAttendance(
            AttendanceMarkRequest request
    );
}