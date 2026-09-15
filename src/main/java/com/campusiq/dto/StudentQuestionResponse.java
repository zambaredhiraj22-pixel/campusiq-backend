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
public class StudentQuestionResponse {

    // Original question-bank ID, also used by StudentAnswerRequest.questionId.
    private Long id;

    private String questionText;

    private String optionA;

    private String optionB;

    private String optionC;

    private String optionD;

    private QuestionCategory category;

    private String technicalSkill;

    // Student's saved choice: A, B, C or D; null means unanswered.
    private String selectedOption;

    // Preserves the constructor currently used by MockTestServiceImpl.
    public StudentQuestionResponse(
            Long id,
            String questionText,
            String optionA,
            String optionB,
            String optionC,
            String optionD,
            QuestionCategory category,
            String technicalSkill) {

        this(id, questionText, optionA, optionB, optionC, optionD,
                category, technicalSkill, null);
    }
}
