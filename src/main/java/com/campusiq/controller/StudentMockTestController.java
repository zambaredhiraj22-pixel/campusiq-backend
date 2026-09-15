package com.campusiq.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusiq.dto.MockTestResponse;
import com.campusiq.dto.StartMockTestRequest;
import com.campusiq.dto.StartMockTestResponse;
import com.campusiq.dto.StudentAnswerRequest;
import com.campusiq.dto.StudentQuestionResponse;
import com.campusiq.dto.SubmitMockTestRequest;
import com.campusiq.dto.SubmitMockTestResponse;
import com.campusiq.service.MockTestService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/student/mock-tests")
@RequiredArgsConstructor
public class StudentMockTestController {

    private final MockTestService mockTestService;

    /*
     * Returns mock tests available to the
     * currently logged-in student.
     *
     * Manual tests:
     * -> active tests remain available normally.
     *
     * AI Personalized tests:
     * -> returned only when specifically
     *    assigned to this student.
     */
    @GetMapping
    public ResponseEntity<List<MockTestResponse>>
            getAvailableMockTests(
                    Authentication authentication) {

        List<MockTestResponse> mockTests =
                mockTestService
                        .getAvailableMockTests(
                                authentication.getName()
                        );

        return ResponseEntity.ok(
                mockTests
        );
    }

    /*
     * Starts either:
     *
     * 1. Manual Mock Test
     *    -> selectedSkill required
     *
     * 2. AI Personalized Mock Test
     *    -> selectedSkill not required
     *    -> backend checks assignment
     *    -> exact mapped questions are used
     */
    @PostMapping("/start")
    public ResponseEntity<StartMockTestResponse>
            startMockTest(
                    Authentication authentication,
                    @Valid
                    @RequestBody
                    StartMockTestRequest request) {

        StartMockTestResponse response =
                mockTestService.startMockTest(
                        authentication.getName(),
                        request
                );

        return ResponseEntity.ok(
                response
        );
    }

    /*
     * Reloads an existing open attempt.
     */
    @GetMapping("/attempts/{testAttemptId}")
    public ResponseEntity<StartMockTestResponse>
            getAttempt(
                    Authentication authentication,
                    @PathVariable("testAttemptId")
                    Long testAttemptId) {

        StartMockTestResponse response =
                mockTestService.getAttempt(
                        authentication.getName(),
                        testAttemptId
                );

        return ResponseEntity.ok(
                response
        );
    }

    /*
     * Saves one answer during the test.
     */
    @PostMapping("/attempts/{testAttemptId}/answers")
    public ResponseEntity<StudentQuestionResponse>
            saveAnswer(
                    Authentication authentication,
                    @PathVariable("testAttemptId")
                    Long testAttemptId,
                    @Valid
                    @RequestBody
                    StudentAnswerRequest request) {

        StudentQuestionResponse response =
                mockTestService.saveAnswer(
                        authentication.getName(),
                        testAttemptId,
                        request
                );

        return ResponseEntity.ok(
                response
        );
    }

    /*
     * Manually submits the student's test.
     */
    @PostMapping("/submit")
    public ResponseEntity<SubmitMockTestResponse>
            submitMockTest(
                    Authentication authentication,
                    @Valid
                    @RequestBody
                    SubmitMockTestRequest request) {

        SubmitMockTestResponse response =
                mockTestService.submitMockTest(
                        authentication.getName(),
                        request
                );

        return ResponseEntity.ok(
                response
        );
    }

    /*
     * Returns the result.
     *
     * POST is intentionally retained because
     * requesting the result may finalize an
     * expired/auto-submitted attempt.
     */
    @PostMapping("/attempts/{testAttemptId}/result")
    public ResponseEntity<SubmitMockTestResponse>
            getResult(
                    Authentication authentication,
                    @PathVariable("testAttemptId")
                    Long testAttemptId) {

        SubmitMockTestResponse response =
                mockTestService.getResult(
                        authentication.getName(),
                        testAttemptId
                );

        return ResponseEntity.ok(
                response
        );
    }
}