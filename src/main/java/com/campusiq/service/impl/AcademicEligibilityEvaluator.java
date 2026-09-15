package com.campusiq.service.impl;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.campusiq.entity.CompanyEligibilityCriteria;

@Component
public class AcademicEligibilityEvaluator {

    public void evaluate(
            Double tenthPercentage,
            Double twelfthPercentage,
            Double diplomaPercentage,
            Double cgpa,
            String department,
            Double attendancePercentage,
            CompanyEligibilityCriteria criteria,
            List<String> passedCriteria,
            List<String> failedCriteria) {

        check10th(
                tenthPercentage,
                criteria.getMinimum10thPercentage(),
                passedCriteria,
                failedCriteria
        );

        check12thOrDiploma(
                twelfthPercentage,
                diplomaPercentage,
                criteria,
                passedCriteria,
                failedCriteria
        );

        checkCgpa(
                cgpa,
                criteria.getMinimumCgpa(),
                passedCriteria,
                failedCriteria
        );

        checkDepartment(
                department,
                criteria.getAllowedDepartments(),
                passedCriteria,
                failedCriteria
        );

        checkAttendance(
                attendancePercentage,
                criteria.getMinimumAttendancePercentage(),
                passedCriteria,
                failedCriteria
        );
    }

    private void check10th(
            Double studentPercentage,
            Double requiredPercentage,
            List<String> passedCriteria,
            List<String> failedCriteria) {

        if (requiredPercentage == null) {
            return;
        }

        if (studentPercentage != null
                && studentPercentage >= requiredPercentage) {

            passedCriteria.add(
                    "10th percentage criteria satisfied"
            );

        } else {

            failedCriteria.add(
                    "10th percentage is below company requirement"
            );
        }
    }

    private void check12thOrDiploma(
            Double twelfthPercentage,
            Double diplomaPercentage,
            CompanyEligibilityCriteria criteria,
            List<String> passedCriteria,
            List<String> failedCriteria) {

        if (diplomaPercentage != null) {

            Double requiredDiploma =
                    criteria.getMinimumDiplomaPercentage();

            if (requiredDiploma == null) {
                return;
            }

            if (diplomaPercentage >= requiredDiploma) {

                passedCriteria.add(
                        "Diploma percentage criteria satisfied"
                );

            } else {

                failedCriteria.add(
                        "Diploma percentage is below company requirement"
                );
            }

            return;
        }

        Double required12th =
                criteria.getMinimum12thPercentage();

        if (required12th == null) {
            return;
        }

        if (twelfthPercentage != null
                && twelfthPercentage >= required12th) {

            passedCriteria.add(
                    "12th percentage criteria satisfied"
            );

        } else {

            failedCriteria.add(
                    "12th percentage is below company requirement"
            );
        }
    }

    private void checkCgpa(
            Double studentCgpa,
            Double requiredCgpa,
            List<String> passedCriteria,
            List<String> failedCriteria) {

        if (requiredCgpa == null) {
            return;
        }

        if (studentCgpa != null
                && studentCgpa >= requiredCgpa) {

            passedCriteria.add(
                    "CGPA criteria satisfied"
            );

        } else {

            failedCriteria.add(
                    "CGPA is below company requirement"
            );
        }
    }

    private void checkDepartment(
            String studentDepartment,
            Set<String> allowedDepartments,
            List<String> passedCriteria,
            List<String> failedCriteria) {

        if (allowedDepartments == null
                || allowedDepartments.isEmpty()) {
            return;
        }

        boolean allowed = false;

        if (studentDepartment != null) {

            for (String department : allowedDepartments) {

                if (department.equalsIgnoreCase(studentDepartment)) {
                    allowed = true;
                    break;
                }
            }
        }

        if (allowed) {

            passedCriteria.add(
                    "Department criteria satisfied"
            );

        } else {

            failedCriteria.add(
                    "Department is not allowed for this company"
            );
        }
    }

    private void checkAttendance(
            Double studentAttendance,
            Double requiredAttendance,
            List<String> passedCriteria,
            List<String> failedCriteria) {

        if (requiredAttendance == null) {
            return;
        }

        if (studentAttendance != null
                && studentAttendance >= requiredAttendance) {

            passedCriteria.add(
                    "Attendance criteria satisfied"
            );

        } else {

            failedCriteria.add(
                    "Attendance is below company requirement"
            );
        }
    }
}