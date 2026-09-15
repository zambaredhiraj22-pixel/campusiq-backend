package com.campusiq.service;

import java.util.List;

import com.campusiq.dto.AiGeneratedQuestionResponse;
import com.campusiq.enums.QuestionCategory;

public interface AiQuestionProvider {

    List<AiGeneratedQuestionResponse> generateQuestions(
            QuestionCategory category,
            String technicalSkill,
            int count
    );
}