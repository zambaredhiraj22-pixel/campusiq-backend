package com.campusiq.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusiq.dto.SkillRequest;
import com.campusiq.dto.SkillResponse;
import com.campusiq.enums.SkillStatus;
import com.campusiq.service.SkillService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @PostMapping("/student/skills")
    public ResponseEntity<SkillResponse> addSkill(
            @Valid @RequestBody SkillRequest request,
            Authentication authentication) {

        SkillResponse response = skillService.addSkill(
                authentication.getName(),
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/student/skills")
    public ResponseEntity<List<SkillResponse>> getMySkills(
            Authentication authentication) {

        return ResponseEntity.ok(
                skillService.getMySkills(
                        authentication.getName()
                )
        );
    }

    @GetMapping("/faculty/skills/pending")
    public ResponseEntity<List<SkillResponse>> getPendingSkills() {

        return ResponseEntity.ok(
                skillService.getPendingSkills()
        );
    }

    /*
     * Faculty:
     *
     * Returns all Faculty-verified technical skills.
     *
     * The React Faculty AI Mock Test page uses this
     * endpoint to:
     *
     * 1. Identify students who have verified skills.
     * 2. Group skills using studentProfileId.
     * 3. Show the selected student's verified skills.
     * 4. Allow Faculty to choose one or more skills
     *    for a personalized AI mock test.
     *
     * SecurityConfig already protects:
     *
     * /api/faculty/**
     *
     * with:
     *
     * hasRole("FACULTY")
     */
    @GetMapping("/faculty/skills/verified")
    public ResponseEntity<List<SkillResponse>> getVerifiedSkills() {

        return ResponseEntity.ok(
                skillService.getVerifiedSkills()
        );
    }

    @PutMapping("/faculty/skills/{skillId}/verify")
    public ResponseEntity<SkillResponse> verifySkill(
            @PathVariable Long skillId) {

        return ResponseEntity.ok(
                skillService.updateSkillStatus(
                        skillId,
                        SkillStatus.VERIFIED
                )
        );
    }

    @PutMapping("/faculty/skills/{skillId}/reject")
    public ResponseEntity<SkillResponse> rejectSkill(
            @PathVariable Long skillId) {

        return ResponseEntity.ok(
                skillService.updateSkillStatus(
                        skillId,
                        SkillStatus.REJECTED
                )
        );
    }
}
