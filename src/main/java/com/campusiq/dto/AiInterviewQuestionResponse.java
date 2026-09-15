package com.campusiq.dto;

import com.campusiq.enums.AiInterviewQuestionType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiInterviewQuestionResponse {

    private Long questionId;

    private String questionText;

    private AiInterviewQuestionType questionType;

    private String technicalSkill;

    private Integer interviewOrder;

    private String savedAnswer;
}