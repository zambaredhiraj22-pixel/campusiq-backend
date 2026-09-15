package com.campusiq.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.campusiq.entity.AttendanceRecord;
import com.campusiq.entity.StudentProfile;

@Repository
public interface AttendanceRecordRepository
        extends JpaRepository<AttendanceRecord, Long> {

    boolean existsByStudentProfileAndAttendanceDate(
            StudentProfile studentProfile,
            LocalDate attendanceDate
    );

    Optional<AttendanceRecord> findByStudentProfileAndAttendanceDate(
            StudentProfile studentProfile,
            LocalDate attendanceDate
    );
    

    long countByStudentProfile(StudentProfile studentProfile);
}