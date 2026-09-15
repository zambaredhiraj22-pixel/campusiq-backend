package com.campusiq.dto;

import com.campusiq.enums.QuestionCategory;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionResponse {

    private Long id;

    private String questionText;

    private String optionA;

    private String optionB;

    private String optionC;

    private String optionD;

    private String correctOption;

    private QuestionCategory category;

    private String technicalSkill;

    public QuestionResponse(
            Long id,
            String questionText,
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            String correctOption,
            QuestionCategory category,
            String technicalSkill) {

        this.id = id;
        this.questionText = questionText;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctOption = correctOption;
        this.category = category;
        this.technicalSkill = technicalSkill;
    }
}