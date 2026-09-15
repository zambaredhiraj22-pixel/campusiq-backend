package com.campusiq.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusiq.dto.ProctoringViolationRequest;
import com.campusiq.dto.ProctoringViolationResponse;
import com.campusiq.dto.TestAttemptRequest;
import com.campusiq.dto.TestAttemptResponse;
import com.campusiq.service.ProctoringService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/proctoring")
@RequiredArgsConstructor
public class ProctoringController {

    private final ProctoringService proctoringService;

    @PostMapping("/start-test")
    public ResponseEntity<TestAttemptResponse> startTest(
            @RequestBody TestAttemptRequest request,
            Authentication authentication) {

        String username = authentication.getName();

        TestAttemptResponse response =
                proctoringService.startTest(request, username);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/violations")
    public ResponseEntity<ProctoringViolationResponse> recordViolation(
            @RequestBody ProctoringViolationRequest request,
            Authentication authentication) {

        String username = authentication.getName();

        ProctoringViolationResponse response =
                proctoringService.recordViolation(request, username);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/attempts/{testAttemptId}/status")
    public ResponseEntity<TestAttemptResponse> getTestAttemptStatus(
            @PathVariable Long testAttemptId,
            Authentication authentication) {

        String username = authentication.getName();

        TestAttemptResponse response =
                proctoringService.getTestAttemptStatus(
                        testAttemptId,
                        username);

        return ResponseEntity.ok(response);
    }
}