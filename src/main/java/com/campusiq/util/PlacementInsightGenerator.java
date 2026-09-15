package com.campusiq.util;

import java.util.ArrayList;
import java.util.List;

public final class PlacementInsightGenerator {

    private PlacementInsightGenerator() {
    }

    public static List<String> generateStrengths(
            double academicScore,
            double attendanceScore,
            double skillScore,
            double assessmentScore,
            double integrityScore) {

        List<String> strengths = new ArrayList<>();

        if (academicScore >= 75.0) {
            strengths.add(
                    "Strong academic performance"
            );
        }

        if (attendanceScore >= 80.0) {
            strengths.add(
                    "Good attendance consistency"
            );
        }

        if (skillScore >= 75.0) {
            strengths.add(
                    "Strong faculty-verified technical skills"
            );
        }

        if (assessmentScore >= 75.0) {
            strengths.add(
                    "Good mock test performance"
            );
        }

        if (integrityScore >= 90.0) {
            strengths.add(
                    "Excellent assessment integrity"
            );
        }

        if (strengths.isEmpty()) {
            strengths.add(
                    "Student has started building placement readiness"
            );
        }

        return strengths;
    }

    public static List<String> generateWeakAreas(
            double academicScore,
            double attendanceScore,
            double skillScore,
            double assessmentScore,
            double integrityScore) {

        List<String> weakAreas = new ArrayList<>();

        if (academicScore < 60.0) {
            weakAreas.add(
                    "Academic performance"
            );
        }

        if (attendanceScore < 75.0) {
            weakAreas.add(
                    "Attendance"
            );
        }

        if (skillScore < 60.0) {
            weakAreas.add(
                    "Verified technical skills"
            );
        }

        if (assessmentScore < 65.0) {
            weakAreas.add(
                    "Mock test performance"
            );
        }

        if (integrityScore < 80.0) {
            weakAreas.add(
                    "Assessment integrity"
            );
        }

        return weakAreas;
    }

    public static List<String> generateRecommendations(
            double academicScore,
            double attendanceScore,
            double skillScore,
            double assessmentScore,
            double integrityScore) {

        List<String> recommendations =
                new ArrayList<>();

        if (academicScore < 60.0) {

            recommendations.add(
                    "Improve academic performance and maintain consistent semester results."
            );
        }

        if (attendanceScore < 75.0) {

            recommendations.add(
                    "Improve attendance and maintain at least 75 percent attendance."
            );
        }

        if (skillScore < 60.0) {

            recommendations.add(
                    "Add relevant technical skills and get them verified by faculty."
            );
        }

        if (assessmentScore < 65.0) {

            recommendations.add(
                    "Practice more aptitude, reasoning and technical mock test questions."
            );
        }

        if (integrityScore < 80.0) {

            recommendations.add(
                    "Avoid proctoring violations and follow assessment rules carefully."
            );
        }

        if (recommendations.isEmpty()) {

            recommendations.add(
                    "Maintain your current performance and continue practicing for placement drives."
            );
        }

        return recommendations;
    }
}