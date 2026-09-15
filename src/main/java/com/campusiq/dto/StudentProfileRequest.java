package com.campusiq.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StudentProfileRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Pattern(
        regexp = "^[0-9]{10,15}$",
        message = "Phone number must contain 10 to 15 digits"
    )
    private String phone;

    @NotBlank(message = "Department is required")
    @Size(max = 50, message = "Department must not exceed 50 characters")
    private String department;

    @NotBlank(message = "Year of study is required")
    @Size(max = 20, message = "Year of study must not exceed 20 characters")
    private String yearOfStudy;

    @DecimalMin(value = "0.0", message = "CGPA cannot be less than 0")
    @DecimalMax(value = "10.0", message = "CGPA cannot be greater than 10")
    private Double cgpa;

    @DecimalMin(value = "0.0", message = "10th percentage cannot be less than 0")
    @DecimalMax(value = "100.0", message = "10th percentage cannot be greater than 100")
    private Double tenthPercentage;

    @DecimalMin(value = "0.0", message = "12th percentage cannot be less than 0")
    @DecimalMax(value = "100.0", message = "12th percentage cannot be greater than 100")
    private Double twelfthPercentage;

    @DecimalMin(value = "0.0", message = "Diploma percentage cannot be less than 0")
    @DecimalMax(value = "100.0", message = "Diploma percentage cannot be greater than 100")
    private Double diplomaPercentage;

    @DecimalMin(value = "0.0", message = "Attendance cannot be less than 0")
    @DecimalMax(value = "100.0", message = "Attendance cannot be greater than 100")
    private Double attendancePercentage;
}