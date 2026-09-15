package com.campusiq.util;

import com.campusiq.enums.ReadinessStatus;

public final class PlacementReadinessCalculator {

    private static final double ACADEMIC_WEIGHT = 0.25;

    private static final double ATTENDANCE_WEIGHT = 0.15;

    private static final double SKILL_WEIGHT = 0.20;

    private static final double ASSESSMENT_WEIGHT = 0.30;

    private static final double INTEGRITY_WEIGHT = 0.10;

    private PlacementReadinessCalculator() {
    }

    public static double calculateReadinessScore(
            double academicScore,
            double attendanceScore,
            double skillScore,
            double assessmentScore,
            double integrityScore) {

        academicScore = normalizeScore(academicScore);
        attendanceScore = normalizeScore(attendanceScore);
        skillScore = normalizeScore(skillScore);
        assessmentScore = normalizeScore(assessmentScore);
        integrityScore = normalizeScore(integrityScore);

        double readinessScore =
                (academicScore * ACADEMIC_WEIGHT)
                + (attendanceScore * ATTENDANCE_WEIGHT)
                + (skillScore * SKILL_WEIGHT)
                + (assessmentScore * ASSESSMENT_WEIGHT)
                + (integrityScore * INTEGRITY_WEIGHT);

        return Math.round(readinessScore * 100.0) / 100.0;
    }

    public static ReadinessStatus determineReadinessStatus(
            double readinessScore) {

        readinessScore =
                normalizeScore(readinessScore);

        if (readinessScore >= 80.0) {

            return ReadinessStatus.READY;
        }

        if (readinessScore >= 60.0) {

            return ReadinessStatus.ALMOST_READY;
        }

        return ReadinessStatus.NEEDS_IMPROVEMENT;
    }

    private static double normalizeScore(
            double score) {

        if (score < 0.0) {

            return 0.0;
        }

        if (score > 100.0) {

            return 100.0;
        }

        return score;
    }
}