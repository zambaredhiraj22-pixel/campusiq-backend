package com.campusiq.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceMarkResponse {

    private Long attendanceId;

    private Long studentProfileId;

    private String studentName;

    private LocalDate attendanceDate;

    private LocalDateTime markedAt;

    private String status;

    private String message;
}