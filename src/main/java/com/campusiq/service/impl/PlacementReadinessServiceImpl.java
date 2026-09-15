package com.campusiq.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.campusiq.dto.PlacementReadinessResponse;
import com.campusiq.entity.MockTestResult;
import com.campusiq.entity.ProctoringViolation;
import com.campusiq.entity.Skill;
import com.campusiq.entity.StudentProfile;
import com.campusiq.entity.TestAttempt;
import com.campusiq.enums.ReadinessStatus;
import com.campusiq.enums.SkillStatus;
import com.campusiq.repository.MockTestResultRepository;
import com.campusiq.repository.ProctoringViolationRepository;
import com.campusiq.repository.SkillRepository;
import com.campusiq.repository.StudentProfileRepository;
import com.campusiq.repository.TestAttemptRepository;
import com.campusiq.service.PlacementReadinessService;
import com.campusiq.util.PlacementComponentScoreCalculator;
import com.campusiq.util.PlacementInsightGenerator;
import com.campusiq.util.PlacementReadinessCalculator;

@Service
public class PlacementReadinessServiceImpl
        implements PlacementReadinessService {

    private final StudentProfileRepository studentProfileRepository;

    private final SkillRepository skillRepository;

    private final MockTestResultRepository mockTestResultRepository;

    private final TestAttemptRepository testAttemptRepository;

    private final ProctoringViolationRepository proctoringViolationRepository;

    public PlacementReadinessServiceImpl(
            StudentProfileRepository studentProfileRepository,
            SkillRepository skillRepository,
            MockTestResultRepository mockTestResultRepository,
            TestAttemptRepository testAttemptRepository,
            ProctoringViolationRepository proctoringViolationRepository) {

        this.studentProfileRepository =
                studentProfileRepository;

        this.skillRepository =
                skillRepository;

        this.mockTestResultRepository =
                mockTestResultRepository;

        this.testAttemptRepository =
                testAttemptRepository;

        this.proctoringViolationRepository =
                proctoringViolationRepository;
    }

    @Override
    public PlacementReadinessResponse getPlacementReadiness(
            Long studentProfileId) {

        // =============================================
        // 1. FIND STUDENT PROFILE
        // =============================================

        StudentProfile studentProfile =
                studentProfileRepository
                        .findById(studentProfileId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Student profile not found with id: "
                                                + studentProfileId
                                )
                        );

        // =============================================
        // 2. CALCULATE ACADEMIC SCORE
        // =============================================

        double academicScore =
                PlacementComponentScoreCalculator
                        .calculateAcademicScore(
                                studentProfile.getCgpa(),
                                studentProfile.getTenthPercentage(),
                                studentProfile.getTwelfthPercentage(),
                                studentProfile.getDiplomaPercentage()
                        );

        // =============================================
        // 3. CALCULATE ATTENDANCE SCORE
        // =============================================

        double attendanceScore =
                PlacementComponentScoreCalculator
                        .calculateAttendanceScore(
                                studentProfile
                                        .getAttendancePercentage()
                        );

        // =============================================
        // 4. CALCULATE VERIFIED SKILL SCORE
        // =============================================

        List<Skill> verifiedSkills =
                skillRepository
                        .findByStudentProfileAndStatus(
                                studentProfile,
                                SkillStatus.VERIFIED
                        );

        long verifiedSkillCount =
                verifiedSkills.size();

        double skillScore =
                PlacementComponentScoreCalculator
                        .calculateSkillScore(
                                verifiedSkillCount
                        );

        // =============================================
        // 5. GET LATEST MOCK TEST RESULT
        // =============================================

        Optional<MockTestResult> latestResult =
                mockTestResultRepository
                        .findTopByStudentProfileOrderByAttemptedAtDesc(
                                studentProfile
                        );

        Double latestPercentage =
                latestResult
                        .map(MockTestResult::getPercentage)
                        .orElse(0.0);

        double assessmentScore =
                PlacementComponentScoreCalculator
                        .calculateAssessmentScore(
                                latestPercentage
                        );

        // =============================================
        // 6. CALCULATE PROCTORING INTEGRITY SCORE
        // =============================================

        Optional<TestAttempt> latestAttempt =
                testAttemptRepository
                        .findTopByStudentProfileAndCompletedTrueOrderBySubmittedAtDesc(
                                studentProfile
                        );

        double integrityScore = 0.0;

        if (latestAttempt.isPresent()) {

            Long testAttemptId =
                    latestAttempt.get().getId();

            List<ProctoringViolation> violations =
                    proctoringViolationRepository
                            .findByTestAttemptId(
                                    testAttemptId
                            );

            integrityScore =
                    PlacementComponentScoreCalculator
                            .calculateIntegrityScore(
                                    violations.size()
                            );
        }

        // =============================================
        // 7. CALCULATE FINAL READINESS SCORE
        // =============================================

        double readinessScore =
                PlacementReadinessCalculator
                        .calculateReadinessScore(
                                academicScore,
                                attendanceScore,
                                skillScore,
                                assessmentScore,
                                integrityScore
                        );

        // =============================================
        // 8. DETERMINE READINESS STATUS
        // =============================================

        ReadinessStatus readinessStatus =
                PlacementReadinessCalculator
                        .determineReadinessStatus(
                                readinessScore
                        );

        // =============================================
        // 9. GENERATE STRENGTHS
        // =============================================

        List<String> strengths =
                PlacementInsightGenerator
                        .generateStrengths(
                                academicScore,
                                attendanceScore,
                                skillScore,
                                assessmentScore,
                                integrityScore
                        );

        // =============================================
        // 10. GENERATE WEAK AREAS
        // =============================================

        List<String> weakAreas =
                PlacementInsightGenerator
                        .generateWeakAreas(
                                academicScore,
                                attendanceScore,
                                skillScore,
                                assessmentScore,
                                integrityScore
                        );

        // =============================================
        // 11. GENERATE RECOMMENDATIONS
        // =============================================

        List<String> recommendations =
                PlacementInsightGenerator
                        .generateRecommendations(
                                academicScore,
                                attendanceScore,
                                skillScore,
                                assessmentScore,
                                integrityScore
                        );

        // =============================================
        // 12. RETURN FINAL RESPONSE
        // =============================================

        return new PlacementReadinessResponse(
                studentProfile.getId(),
                studentProfile.getFullName(),
                readinessScore,
                readinessStatus,
                academicScore,
                attendanceScore,
                skillScore,
                assessmentScore,
                integrityScore,
                strengths,
                weakAreas,
                recommendations
        );
    }
}