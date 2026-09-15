package com.campusiq.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StartMockTestRequest {

    private Long mockTestId;

    private String selectedSkill;
}