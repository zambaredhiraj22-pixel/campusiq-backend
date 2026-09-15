package com.campusiq.service;

import java.util.List;

import com.campusiq.dto.SkillRequest;
import com.campusiq.dto.SkillResponse;
import com.campusiq.enums.SkillStatus;

public interface SkillService {

    /*
     * Student adds a new technical skill.
     *
     * Newly added skills are initially
     * stored with PENDING status.
     */
    SkillResponse addSkill(
            String username,
            SkillRequest request
    );

    /*
     * Returns all skills belonging to
     * the currently logged-in student.
     */
    List<SkillResponse> getMySkills(
            String username
    );

    /*
     * Faculty:
     *
     * Returns all skills waiting for
     * Faculty verification.
     */
    List<SkillResponse> getPendingSkills();

    /*
     * Faculty:
     *
     * Returns all Faculty-verified skills.
     *
     * This is required by the personalized
     * AI Mock Test frontend so Faculty can:
     *
     * 1. See students who have verified skills.
     * 2. Select one student.
     * 3. See that student's verified skills.
     * 4. Select one or multiple verified skills.
     * 5. Generate the personalized AI test.
     *
     * SkillResponse already contains:
     *
     * - studentProfileId
     * - studentName
     * - username
     * - skillName
     * - proficiencyLevel
     * - status
     */
    List<SkillResponse> getVerifiedSkills();

    /*
     * Faculty verifies or rejects
     * a student's submitted skill.
     */
    SkillResponse updateSkillStatus(
            Long skillId,
            SkillStatus status
    );
}