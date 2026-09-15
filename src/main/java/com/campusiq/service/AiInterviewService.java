package com.campusiq.service;

import java.util.List;

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

public interface AiInterviewService {

    StudentInterviewProfileResponse saveOrUpdateProfile(
            String username,
            StudentInterviewProfileRequest request
    );

    StudentInterviewProfileResponse uploadResume(
            String username,
            MultipartFile file
    );

    StudentInterviewProfileResponse getProfile(
            String username
    );

    AiInterviewEligibilityResponse checkEligibility(
            String username
    );

    AiInterviewPrepareResponse prepareInterview(
            String username,
            Long mockTestResultId
    );

    AiInterviewStartResponse startInterview(
            String username,
            Long interviewSessionId
    );

    AiInterviewStartResponse getInterview(
            String username,
            Long interviewSessionId
    );

    AiInterviewAnswerResponse saveAnswer(
            String username,
            Long interviewSessionId,
            AiInterviewAnswerRequest request
    );

    AiInterviewViolationResponse reportViolation(
            String username,
            Long interviewSessionId,
            AiInterviewViolationRequest request
    );

    List<AiInterviewViolationResponse> getViolationHistory(
            String username,
            Long interviewSessionId
    );

    AiInterviewResultResponse submitInterview(
            String username,
            Long interviewSessionId
    );

    AiInterviewResultResponse getResult(
            String username,
            Long interviewSessionId
    );

    void finalizeExpiredInterviews();
}