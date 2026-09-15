package com.campusiq.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusiq.dto.AiMockTestCreateRequest;
import com.campusiq.dto.AiMockTestResponse;
import com.campusiq.service.AiMockTestService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/faculty/ai/mock-tests")
@RequiredArgsConstructor
public class FacultyAiMockTestController {

    private final AiMockTestService aiMockTestService;

    /*
     * Creates a complete personalized AI Mock Test
     * for one selected student.
     *
     * Backend service validates:
     *
     * 1. Student exists.
     * 2. Selected technical skills are
     *    faculty-verified for that student.
     * 3. Exactly 60 unique approved questions exist.
     * 4. Question structure is:
     *      15 Aptitude
     *      15 Reasoning
     *      30 Technical
     * 5. Technical questions are balanced
     *    across selected verified skills.
     * 6. MockTest is created with:
     *      pass percentage = 65
     *      active = true
     * 7. Exact 60 questions are mapped.
     * 8. Test is assigned only to
     *    the selected student.
     */
    @PostMapping("/create")
    public ResponseEntity<AiMockTestResponse>
            createPersonalizedMockTest(
                    @Valid
                    @RequestBody
                    AiMockTestCreateRequest request) {

        AiMockTestResponse response =
                aiMockTestService
                        .createPersonalizedMockTest(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}