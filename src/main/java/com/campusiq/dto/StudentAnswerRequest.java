package com.campusiq.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StudentAnswerRequest {

    private Long questionId;

    private String selectedOption;
}