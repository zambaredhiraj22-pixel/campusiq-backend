package com.campusiq.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.campusiq.dto.AiInterviewAnswerRequest;
import com.campusiq.dto.AiInterviewAnswerResponse;
import com.campusiq.dto.AiInterviewEligibilityResponse;
import com.campusiq.dto.AiInterviewPrepareResponse;
import com.campusiq.dto.AiInterviewResultResponse;
import com.campusiq.dto.AiInterviewStartResponse;
import com.campusiq.dto.AiInterviewViolationRequest;
import com.campusiq.dto.AiInterviewViolationResponse;
import com.campusiq.dto.StudentInterviewProfileRequest;
import com.campusiq.dto.StudentInterviewProfileResponse;
import com.campusiq.service.AiInterviewService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/student/ai-interview")
@RequiredArgsConstructor
public class AiInterviewController {

    private final AiInterviewService aiInterviewService;

    @PutMapping("/profile")
    public ResponseEntity<StudentInterviewProfileResponse>
            saveOrUpdateProfile(
                    Authentication authentication,
                    @Valid @RequestBody
                    StudentInterviewProfileRequest request) {

        return ResponseEntity.ok(
                aiInterviewService.saveOrUpdateProfile(
                        authentication.getName(),
                        request
                )
        );
    }

    @PostMapping(
            value = "/profile/resume",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<StudentInterviewProfileResponse>
            uploadResume(
                    Authentication authentication,
                    @RequestParam("file")
                    MultipartFile file) {

        return ResponseEntity.ok(
                aiInterviewService.uploadResume(
                        authentication.getName(),
                        file
                )
        );
    }

    @GetMapping("/profile")
    public ResponseEntity<StudentInterviewProfileResponse>
            getProfile(
                    Authentication authentication) {

        return ResponseEntity.ok(
                aiInterviewService.getProfile(
                        authentication.getName()
                )
        );
    }

    @GetMapping("/eligibility")
    public ResponseEntity<AiInterviewEligibilityResponse>
            checkEligibility(
                    Authentication authentication) {

        return ResponseEntity.ok(
                aiInterviewService.checkEligibility(
                        authentication.getName()
                )
        );
    }

    @PostMapping("/prepare/{mockTestResultId}")
    public ResponseEntity<AiInterviewPrepareResponse>
            prepareInterview(
                    Authentication authentication,
                    @PathVariable
                    Long mockTestResultId) {

        AiInterviewPrepareResponse response =
                aiInterviewService.prepareInterview(
                        authentication.getName(),
                        mockTestResultId
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/{interviewSessionId}/start")
    public ResponseEntity<AiInterviewStartResponse>
            startInterview(
                    Authentication authentication,
                    @PathVariable
                    Long interviewSessionId) {

        return ResponseEntity.ok(
                aiInterviewService.startInterview(
                        authentication.getName(),
                        interviewSessionId
                )
        );
    }

    @GetMapping("/{interviewSessionId}")
    public ResponseEntity<AiInterviewStartResponse>
            getInterview(
                    Authentication authentication,
                    @PathVariable
                    Long interviewSessionId) {

        return ResponseEntity.ok(
                aiInterviewService.getInterview(
                        authentication.getName(),
                        interviewSessionId
                )
        );
    }

    @PostMapping("/{interviewSessionId}/answers")
    public ResponseEntity<AiInterviewAnswerResponse>
            saveAnswer(
                    Authentication authentication,
                    @PathVariable
                    Long interviewSessionId,
                    @Valid @RequestBody
                    AiInterviewAnswerRequest request) {

        return ResponseEntity.ok(
                aiInterviewService.saveAnswer(
                        authentication.getName(),
                        interviewSessionId,
                        request
                )
        );
    }

    @PostMapping("/{interviewSessionId}/violations")
    public ResponseEntity<AiInterviewViolationResponse>
            reportViolation(
                    Authentication authentication,
                    @PathVariable
                    Long interviewSessionId,
                    @Valid @RequestBody
                    AiInterviewViolationRequest request) {

        return ResponseEntity.ok(
                aiInterviewService.reportViolation(
                        authentication.getName(),
                        interviewSessionId,
                        request
                )
        );
    }

    @GetMapping("/{interviewSessionId}/violations")
    public ResponseEntity<List<AiInterviewViolationResponse>>
            getViolationHistory(
                    Authentication authentication,
                    @PathVariable
                    Long interviewSessionId) {

        return ResponseEntity.ok(
                aiInterviewService.getViolationHistory(
                        authentication.getName(),
                        interviewSessionId
                )
        );
    }

    @PostMapping("/{interviewSessionId}/submit")
    public ResponseEntity<AiInterviewResultResponse>
            submitInterview(
                    Authentication authentication,
                    @PathVariable
                    Long interviewSessionId) {

        return ResponseEntity.ok(
                aiInterviewService.submitInterview(
                        authentication.getName(),
                        interviewSessionId
                )
        );
    }

    @GetMapping("/{interviewSessionId}/result")
    public ResponseEntity<AiInterviewResultResponse>
            getResult(
                    Authentication authentication,
                    @PathVariable
                    Long interviewSessionId) {

        return ResponseEntity.ok(
                aiInterviewService.getResult(
                        authentication.getName(),
                        interviewSessionId
                )
        );
    }
}