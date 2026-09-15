package com.campusiq.dto;

import com.campusiq.enums.SkillStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SkillResponse {

    private Long id;

    private String skillName;

    private String proficiencyLevel;

    private String evidenceUrl;

    private SkillStatus status;

    private Long studentProfileId;

    private String studentName;

    private String username;
}