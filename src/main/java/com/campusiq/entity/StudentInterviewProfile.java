package com.campusiq.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "student_interview_profiles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_interview_profile_student",
                        columnNames = "student_profile_id"
                )
        }
)
public class StudentInterviewProfile {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    /*
     * One student has only one active
     * interview preparation profile.
     */
    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "student_profile_id",
            nullable = false,
            unique = true
    )
    private StudentProfile studentProfile;

    /*
     * Original uploaded resume filename.
     *
     * Example:
     * Dhiraj_Zambre_Resume.pdf
     */
    @Column(
            name = "resume_file_name",
            length = 255
    )
    private String resumeFileName;

    /*
     * MIME type of uploaded resume.
     *
     * Example:
     * application/pdf
     */
    @Column(
            name = "resume_content_type",
            length = 100
    )
    private String resumeContentType;

    /*
     * Extracted resume text.
     *
     * AI will use this text instead of
     * sending the physical PDF repeatedly.
     */
    @Lob
    @Column(
            name = "resume_text",
            columnDefinition = "LONGTEXT"
    )
    private String resumeText;

    /*
     * Student's projects.
     *
     * Example:
     *
     * CAMPUS-IQ placement platform,
     * IoT attendance system,
     * Java Spring Boot projects.
     */
    @Lob
    @Column(
            name = "projects",
            columnDefinition = "LONGTEXT"
    )
    private String projects;

    /*
     * Technologies/tools known by the student.
     *
     * Verified skills still come from the
     * existing Skill table.
     *
     * This field is additional interview
     * context such as:
     *
     * Spring Boot,
     * MySQL,
     * React,
     * Git,
     * Postman,
     * ESP32.
     */
    @Lob
    @Column(
            name = "technologies",
            columnDefinition = "LONGTEXT"
    )
    private String technologies;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now =
                LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt =
                LocalDateTime.now();
    }
}