package com.campusiq.service;

import java.util.List;

import com.campusiq.dto.AiGeneratedQuestionResponse;
import com.campusiq.dto.AiQuestionBulkApproveRequest;
import com.campusiq.dto.AiQuestionGenerateRequest;
import com.campusiq.dto.QuestionResponse;

public interface AiQuestionService {

    List<AiGeneratedQuestionResponse> generateQuestions(
            AiQuestionGenerateRequest request
    );

    List<QuestionResponse> approveQuestions(
            AiQuestionBulkApproveRequest request
    );
}