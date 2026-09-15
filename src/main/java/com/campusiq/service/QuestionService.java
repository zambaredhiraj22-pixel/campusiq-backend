package com.campusiq.service;

import java.util.List;

import com.campusiq.dto.QuestionRequest;
import com.campusiq.dto.QuestionResponse;

public interface QuestionService {

    QuestionResponse addQuestion(QuestionRequest request);

    List<QuestionResponse> getAllQuestions();

    QuestionResponse getQuestionById(Long id);

    QuestionResponse updateQuestion(Long id, QuestionRequest request);

    void deleteQuestion(Long id);
}