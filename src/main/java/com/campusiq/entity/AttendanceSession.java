package com.campusiq.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "attendance_sessions",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_attendance_session_date",
            columnNames = {"session_date"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @PrePersist
    public void setSessionDetails() {

        if (sessionDate == null) {
            sessionDate = LocalDate.now();
        }

        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }

        if (active == null) {
            active = true;
        }
    }
}