package com.campusiq.dto;

import com.campusiq.enums.QuestionCategory;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiGeneratedQuestionResponse {

    private String questionText;

    private String optionA;

    private String optionB;

    private String optionC;

    private String optionD;

    private String correctOption;

    private QuestionCategory category;

    private String technicalSkill;
}