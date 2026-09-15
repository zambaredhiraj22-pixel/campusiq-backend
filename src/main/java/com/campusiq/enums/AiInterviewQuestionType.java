package com.campusiq.enums;

public enum AiInterviewQuestionType {

    /*
     * Questions based mainly on the student's
     * faculty-verified technical skills.
     *
     * Examples:
     * Java, Spring Boot, MySQL, React.
     */
    TECHNICAL,

    /*
     * Questions generated from the student's
     * resume, projects and technologies.
     *
     * Examples:
     * Explain your CAMPUS-IQ project.
     * Why did you use JWT?
     * How did ESP32 communicate with backend?
     */
    RESUME_PROJECT,

    /*
     * Behavioral / HR-style questions.
     *
     * Examples:
     * Tell me about a challenge you faced.
     * How do you handle teamwork?
     * How do you manage deadlines?
     */
    BEHAVIORAL
}