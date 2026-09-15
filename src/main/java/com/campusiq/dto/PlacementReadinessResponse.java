package com.campusiq.dto;

import java.util.List;

import com.campusiq.enums.ReadinessStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlacementReadinessResponse {

    private Long studentProfileId;

    private String studentName;

    private Double readinessScore;

    private ReadinessStatus readinessStatus;

    private Double academicScore;

    private Double attendanceScore;

    private Double skillScore;

    private Double assessmentScore;

    private Double integrityScore;

    private List<String> strengths;

    private List<String> weakAreas;

    private List<String> recommendations;
}