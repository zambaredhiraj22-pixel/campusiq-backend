package com.campusiq.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "attendance_records",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_student_attendance_date",
            columnNames = {"student_profile_id", "attendance_date"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "marked_at", nullable = false)
    private LocalDateTime markedAt;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "fingerprint_template_id", nullable = false)
    private Integer fingerprintTemplateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @PrePersist
    public void setAttendanceDetails() {

        if (attendanceDate == null) {
            attendanceDate = LocalDate.now();
        }

        if (markedAt == null) {
            markedAt = LocalDateTime.now();
        }

        if (status == null || status.isBlank()) {
            status = "PRESENT";
        }
    }
}