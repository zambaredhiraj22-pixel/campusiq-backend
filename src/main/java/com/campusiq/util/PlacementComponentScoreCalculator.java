package com.campusiq.util;

import java.util.ArrayList;
import java.util.List;

public final class PlacementComponentScoreCalculator {

    private PlacementComponentScoreCalculator() {
    }

    // =================================================
    // ACADEMIC SCORE
    // =================================================

    public static double calculateAcademicScore(
            Double cgpa,
            Double tenthPercentage,
            Double twelfthPercentage,
            Double diplomaPercentage) {

        List<Double> academicScores =
                new ArrayList<>();

        // CGPA is normally out of 10.
        // Example: 8.01 CGPA -> 80.1 score.
        if (cgpa != null) {

            academicScores.add(
                    normalizeScore(
                            cgpa * 10.0
                    )
            );
        }

        if (tenthPercentage != null) {

            academicScores.add(
                    normalizeScore(
                            tenthPercentage
                    )
            );
        }

        if (twelfthPercentage != null) {

            academicScores.add(
                    normalizeScore(
                            twelfthPercentage
                    )
            );
        }

        if (diplomaPercentage != null) {

            academicScores.add(
                    normalizeScore(
                            diplomaPercentage
                    )
            );
        }

        if (academicScores.isEmpty()) {
            return 0.0;
        }

        double total = 0.0;

        for (double score : academicScores) {
            total += score;
        }

        double average =
                total / academicScores.size();

        return roundScore(average);
    }

    // =================================================
    // ATTENDANCE SCORE
    // =================================================

    public static double calculateAttendanceScore(
            Double attendancePercentage) {

        if (attendancePercentage == null) {
            return 0.0;
        }

        return roundScore(
                normalizeScore(
                        attendancePercentage
                )
        );
    }

    // =================================================
    // VERIFIED SKILL SCORE
    // =================================================

    public static double calculateSkillScore(
            long verifiedSkillCount) {

        if (verifiedSkillCount <= 0) {
            return 0.0;
        }

        if (verifiedSkillCount == 1) {
            return 50.0;
        }

        if (verifiedSkillCount == 2) {
            return 75.0;
        }

        return 100.0;
    }

    // =================================================
    // ASSESSMENT SCORE
    // =================================================

    public static double calculateAssessmentScore(
            Double assessmentPercentage) {

        if (assessmentPercentage == null) {
            return 0.0;
        }

        return roundScore(
                normalizeScore(
                        assessmentPercentage
                )
        );
    }

    // =================================================
    // PROCTORING INTEGRITY SCORE
    // =================================================

    public static double calculateIntegrityScore(
            long violationCount) {

        if (violationCount <= 0) {
            return 100.0;
        }

        if (violationCount == 1) {
            return 85.0;
        }

        if (violationCount == 2) {
            return 65.0;
        }

        return 40.0;
    }

    // =================================================
    // NORMALIZE SCORE
    // =================================================

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

    // =================================================
    // ROUND TO 2 DECIMAL PLACES
    // =================================================

    private static double roundScore(
            double score) {

        return Math.round(
                score * 100.0
        ) / 100.0;
    }
}