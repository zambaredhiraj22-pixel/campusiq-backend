package com.campusiq.service.impl;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class SkillEligibilityEvaluator {

    public void evaluate(
            Set<String> requiredSkills,
            Set<String> verifiedStudentSkills,
            List<String> passedCriteria,
            List<String> failedCriteria) {

        if (requiredSkills == null || requiredSkills.isEmpty()) {
            return;
        }

        for (String requiredSkill : requiredSkills) {

            boolean skillFound = false;

            if (verifiedStudentSkills != null) {

                for (String studentSkill : verifiedStudentSkills) {

                    if (studentSkill.equalsIgnoreCase(requiredSkill)) {

                        skillFound = true;
                        break;
                    }
                }
            }

            if (skillFound) {

                passedCriteria.add(
                        "Required skill "
                                + requiredSkill
                                + " is verified"
                );

            } else {

                failedCriteria.add(
                        "Required skill "
                                + requiredSkill
                                + " is not verified"
                );
            }
        }
    }
}