package com.campusiq.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.campusiq.dto.AiMockTestCreateRequest;
import com.campusiq.dto.AiMockTestResponse;
import com.campusiq.entity.MockTest;
import com.campusiq.entity.MockTestQuestion;
import com.campusiq.entity.Question;
import com.campusiq.entity.StudentMockTestAssignment;
import com.campusiq.entity.StudentProfile;
import com.campusiq.enums.QuestionCategory;
import com.campusiq.enums.SkillStatus;
import com.campusiq.repository.MockTestQuestionRepository;
import com.campusiq.repository.MockTestRepository;
import com.campusiq.repository.QuestionRepository;
import com.campusiq.repository.SkillRepository;
import com.campusiq.repository.StudentMockTestAssignmentRepository;
import com.campusiq.repository.StudentProfileRepository;
import com.campusiq.service.AiMockTestService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiMockTestServiceImpl
        implements AiMockTestService {

    private static final int APTITUDE_COUNT = 15;

    private static final int REASONING_COUNT = 15;

    private static final int TECHNICAL_COUNT = 30;

    private static final int TOTAL_QUESTION_COUNT = 60;

    private static final int PASS_PERCENTAGE = 45;

    private static final int MAX_DURATION_MINUTES = 180;

    private final StudentProfileRepository
            studentProfileRepository;

    private final SkillRepository skillRepository;

    private final QuestionRepository questionRepository;

    private final MockTestRepository mockTestRepository;

    private final MockTestQuestionRepository
            mockTestQuestionRepository;

    private final StudentMockTestAssignmentRepository
            studentMockTestAssignmentRepository;

    @Override
    @Transactional
    public AiMockTestResponse createPersonalizedMockTest(
            AiMockTestCreateRequest request) {

        validateRequest(request);

        StudentProfile student =
                studentProfileRepository
                        .findById(
                                request.getStudentProfileId()
                        )
                        .orElseThrow(() ->
                                error(
                                        HttpStatus.NOT_FOUND,
                                        "Student profile not found."
                                )
                        );

        /*
         * Verify all selected skills against the
         * student's faculty-verified skills.
         */
        List<String> selectedVerifiedSkills =
                validateSelectedSkills(
                        student,
                        request.getSelectedSkills()
                );

        /*
         * Load the exact Faculty-approved
         * Question Bank questions.
         */
        List<Question> questions =
                loadQuestions(
                        request.getQuestionIds()
                );

        /*
         * Revalidate the complete AI assessment
         * blueprint before creating the test.
         */
        validateQuestionStructure(
                questions,
                selectedVerifiedSkills
        );

        /*
         * Keep final test order predictable:
         *
         * 1 - 15  Aptitude
         * 16 - 30 Reasoning
         * 31 - 60 Technical
         */
        List<Question> orderedQuestions =
                buildQuestionOrder(
                        questions,
                        selectedVerifiedSkills
                );

        /*
         * Create the actual MockTest.
         */
        MockTest mockTest =
                new MockTest();

        mockTest.setTitle(
                request.getTitle().trim()
        );

        mockTest.setAptitudeQuestionCount(
                APTITUDE_COUNT
        );

        mockTest.setReasoningQuestionCount(
                REASONING_COUNT
        );

        mockTest.setTechnicalQuestionCount(
                TECHNICAL_COUNT
        );

        mockTest.setDurationMinutes(
                request.getDurationMinutes()
        );

        mockTest.setPassPercentage(
                PASS_PERCENTAGE
        );

        mockTest.setActive(true);

        mockTest =
                mockTestRepository.save(
                        mockTest
                );

        /*
         * Attach the exact 60 approved questions
         * to the newly created MockTest.
         */
        List<MockTestQuestion> mappings =
                new ArrayList<>();

        for (int index = 0;
                index < orderedQuestions.size();
                index++) {

            MockTestQuestion mapping =
                    new MockTestQuestion();

            mapping.setMockTest(
                    mockTest
            );

            mapping.setQuestion(
                    orderedQuestions.get(index)
            );

            mapping.setQuestionOrder(
                    index + 1
            );

            mappings.add(mapping);
        }

        mockTestQuestionRepository
                .saveAll(mappings);

        /*
         * Defensive verification.
         *
         * We expect exactly 60 mappings.
         */
        if (mappings.size()
                != TOTAL_QUESTION_COUNT) {

            throw error(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Personalized mock test question mapping failed."
            );
        }

        /*
         * Assign this personalized test
         * only to the selected student.
         */
        StudentMockTestAssignment assignment =
                new StudentMockTestAssignment();

        assignment.setStudentProfile(
                student
        );

        assignment.setMockTest(
                mockTest
        );

        assignment.setActive(true);

        assignment =
                studentMockTestAssignmentRepository
                        .save(assignment);

        /*
         * Build final creation response.
         */
        AiMockTestResponse response =
                new AiMockTestResponse();

        response.setMockTestId(
                mockTest.getId()
        );

        response.setAssignmentId(
                assignment.getId()
        );

        response.setStudentProfileId(
                student.getId()
        );

        response.setTitle(
                mockTest.getTitle()
        );

        response.setSelectedSkills(
                selectedVerifiedSkills
        );

        response.setAptitudeQuestionCount(
                APTITUDE_COUNT
        );

        response.setReasoningQuestionCount(
                REASONING_COUNT
        );

        response.setTechnicalQuestionCount(
                TECHNICAL_COUNT
        );

        response.setTotalQuestions(
                TOTAL_QUESTION_COUNT
        );

        response.setDurationMinutes(
                mockTest.getDurationMinutes()
        );

        response.setPassPercentage(
                mockTest.getPassPercentage()
        );

        response.setActive(
                mockTest.isActive()
        );

        response.setAssignedAt(
                assignment.getAssignedAt()
        );

        return response;
    }

    /*
     * -------------------------------------------------------
     * MAIN REQUEST VALIDATION
     * -------------------------------------------------------
     */
    private void validateRequest(
            AiMockTestCreateRequest request) {

        if (request == null) {

            throw error(
                    HttpStatus.BAD_REQUEST,
                    "AI mock test creation request is required."
            );
        }

        if (request.getStudentProfileId() == null ||
                request.getStudentProfileId() <= 0) {

            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Student profile ID must be a positive number."
            );
        }

        if (request.getTitle() == null ||
                request.getTitle().isBlank()) {

            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Mock test title is required."
            );
        }

        if (request.getSelectedSkills() == null ||
                request.getSelectedSkills().isEmpty()) {

            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Select at least one verified technical skill."
            );
        }

        if (request.getSelectedSkills().size()
                > TECHNICAL_COUNT) {

            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Maximum "
                            + TECHNICAL_COUNT
                            + " technical skills can be selected."
            );
        }

        if (request.getQuestionIds() == null ||
                request.getQuestionIds().size()
                        != TOTAL_QUESTION_COUNT) {

            throw error(
                    HttpStatus.BAD_REQUEST,
                    "AI mock test must contain exactly "
                            + TOTAL_QUESTION_COUNT
                            + " question IDs."
            );
        }

        if (request.getDurationMinutes() == null ||
                request.getDurationMinutes() <= 0 ||
                request.getDurationMinutes()
                        > MAX_DURATION_MINUTES) {

            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Test duration must be between 1 and "
                            + MAX_DURATION_MINUTES
                            + " minutes."
            );
        }
    }

    /*
     * -------------------------------------------------------
     * VERIFIED SKILL VALIDATION
     * -------------------------------------------------------
     */
    private List<String> validateSelectedSkills(
            StudentProfile student,
            List<String> requestedSkills) {

        List<String> verifiedSkills =
                new ArrayList<>();

        Set<String> normalizedSkills =
                new HashSet<>();

        for (String requestedSkill
                : requestedSkills) {

            if (requestedSkill == null ||
                    requestedSkill.isBlank()) {

                throw error(
                        HttpStatus.BAD_REQUEST,
                        "Selected skill cannot be blank."
                );
            }

            String cleanSkill =
                    requestedSkill.trim();

            String normalizedSkill =
                    normalize(cleanSkill);

            /*
             * Java + java must not be treated
             * as two different selected skills.
             */
            if (!normalizedSkills.add(
                    normalizedSkill)) {

                throw error(
                        HttpStatus.BAD_REQUEST,
                        "Duplicate selected skill: "
                                + cleanSkill
                );
            }

            boolean verified =
                    skillRepository
                            .existsByStudentProfileAndSkillNameIgnoreCaseAndStatus(
                                    student,
                                    cleanSkill,
                                    SkillStatus.VERIFIED
                            );

            if (!verified) {

                throw error(
                        HttpStatus.BAD_REQUEST,
                        "Skill '"
                                + cleanSkill
                                + "' is not faculty-verified for this student."
                );
            }

            verifiedSkills.add(
                    cleanSkill
            );
        }

        return verifiedSkills;
    }

    /*
     * -------------------------------------------------------
     * LOAD EXACT 60 QUESTIONS
     * -------------------------------------------------------
     */
    private List<Question> loadQuestions(
            List<Long> questionIds) {

        LinkedHashSet<Long> uniqueIds =
                new LinkedHashSet<>();

        for (Long questionId
                : questionIds) {

            if (questionId == null ||
                    questionId <= 0) {

                throw error(
                        HttpStatus.BAD_REQUEST,
                        "Every question ID must be a positive number."
                );
            }

            if (!uniqueIds.add(
                    questionId)) {

                throw error(
                        HttpStatus.BAD_REQUEST,
                        "Duplicate question ID found: "
                                + questionId
                );
            }
        }

        if (uniqueIds.size()
                != TOTAL_QUESTION_COUNT) {

            throw error(
                    HttpStatus.BAD_REQUEST,
                    "AI mock test requires exactly "
                            + TOTAL_QUESTION_COUNT
                            + " unique questions."
            );
        }

        List<Question> foundQuestions =
                questionRepository
                        .findAllById(uniqueIds);

        if (foundQuestions.size()
                != TOTAL_QUESTION_COUNT) {

            throw error(
                    HttpStatus.BAD_REQUEST,
                    "One or more approved questions do not exist in the Question Bank."
            );
        }

        Map<Long, Question> questionMap =
                new HashMap<>();

        for (Question question
                : foundQuestions) {

            questionMap.put(
                    question.getId(),
                    question
            );
        }

        /*
         * Rebuild using request order.
         * JPA findAllById does not guarantee
         * the same order as input IDs.
         */
        List<Question> orderedByRequest =
                new ArrayList<>();

        for (Long questionId
                : questionIds) {

            Question question =
                    questionMap.get(
                            questionId
                    );

            if (question == null) {

                throw error(
                        HttpStatus.BAD_REQUEST,
                        "Question not found: "
                                + questionId
                );
            }

            orderedByRequest.add(
                    question
            );
        }

        return orderedByRequest;
    }

    /*
     * -------------------------------------------------------
     * 15 + 15 + 30 VALIDATION
     * -------------------------------------------------------
     */
    private void validateQuestionStructure(
            List<Question> questions,
            List<String> selectedSkills) {

        int aptitudeCount = 0;

        int reasoningCount = 0;

        int technicalCount = 0;

        /*
         * Normalized selected skill
         * -> original clean skill name
         */
        Map<String, String>
                selectedSkillMap =
                new LinkedHashMap<>();

        for (String skill
                : selectedSkills) {

            selectedSkillMap.put(
                    normalize(skill),
                    skill
            );
        }

        /*
         * Actual number of technical questions
         * for each selected skill.
         */
        Map<String, Integer>
                actualTechnicalCounts =
                new LinkedHashMap<>();

        for (String skill
                : selectedSkills) {

            actualTechnicalCounts.put(
                    skill,
                    0
            );
        }

        for (Question question
                : questions) {

            if (question.getCategory()
                    == null) {

                throw error(
                        HttpStatus.BAD_REQUEST,
                        "A selected question has no category."
                );
            }

            if (question.getCategory()
                    == QuestionCategory.APTITUDE) {

                aptitudeCount++;

                continue;
            }

            if (question.getCategory()
                    == QuestionCategory.REASONING) {

                reasoningCount++;

                continue;
            }

            if (question.getCategory()
                    == QuestionCategory.TECHNICAL) {

                technicalCount++;

                if (question.getTechnicalSkill()
                        == null ||
                        question.getTechnicalSkill()
                                .isBlank()) {

                    throw error(
                            HttpStatus.BAD_REQUEST,
                            "Every technical question must have a technical skill."
                    );
                }

                String normalizedQuestionSkill =
                        normalize(
                                question.getTechnicalSkill()
                        );

                String selectedSkill =
                        selectedSkillMap.get(
                                normalizedQuestionSkill
                        );

                if (selectedSkill == null) {

                    throw error(
                            HttpStatus.BAD_REQUEST,
                            "Technical question ID "
                                    + question.getId()
                                    + " uses skill '"
                                    + question.getTechnicalSkill()
                                    + "', which is not one of the selected verified skills."
                    );
                }

                actualTechnicalCounts.put(
                        selectedSkill,
                        actualTechnicalCounts.get(
                                selectedSkill
                        ) + 1
                );

                continue;
            }

            /*
             * AI personalized mock test currently
             * supports only these three sections.
             */
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported question category found in AI mock test."
            );
        }

        if (aptitudeCount
                != APTITUDE_COUNT) {

            throw error(
                    HttpStatus.BAD_REQUEST,
                    "AI mock test must contain exactly "
                            + APTITUDE_COUNT
                            + " aptitude questions, but received "
                            + aptitudeCount
                            + "."
            );
        }

        if (reasoningCount
                != REASONING_COUNT) {

            throw error(
                    HttpStatus.BAD_REQUEST,
                    "AI mock test must contain exactly "
                            + REASONING_COUNT
                            + " reasoning questions, but received "
                            + reasoningCount
                            + "."
            );
        }

        if (technicalCount
                != TECHNICAL_COUNT) {

            throw error(
                    HttpStatus.BAD_REQUEST,
                    "AI mock test must contain exactly "
                            + TECHNICAL_COUNT
                            + " technical questions, but received "
                            + technicalCount
                            + "."
            );
        }

        validateTechnicalDistribution(
                selectedSkills,
                actualTechnicalCounts
        );
    }

    /*
     * -------------------------------------------------------
     * BALANCED TECHNICAL DISTRIBUTION
     * -------------------------------------------------------
     */
    private void validateTechnicalDistribution(
            List<String> selectedSkills,
            Map<String, Integer> actualCounts) {

        int skillCount =
                selectedSkills.size();

        int baseCount =
                TECHNICAL_COUNT
                        / skillCount;

        int remainder =
                TECHNICAL_COUNT
                        % skillCount;

        for (int index = 0;
                index < selectedSkills.size();
                index++) {

            String skill =
                    selectedSkills.get(index);

            int expectedCount =
                    baseCount;

            if (index < remainder) {
                expectedCount++;
            }

            int actualCount =
                    actualCounts.getOrDefault(
                            skill,
                            0
                    );

            if (actualCount
                    != expectedCount) {

                throw error(
                        HttpStatus.BAD_REQUEST,
                        "Technical questions for '"
                                + skill
                                + "' must be exactly "
                                + expectedCount
                                + ", but received "
                                + actualCount
                                + "."
                );
            }
        }
    }

    /*
     * -------------------------------------------------------
     * FINAL QUESTION ORDER
     * -------------------------------------------------------
     */
    private List<Question> buildQuestionOrder(
            List<Question> questions,
            List<String> selectedSkills) {

        List<Question> ordered =
                new ArrayList<>();

        /*
         * First 15 Aptitude.
         */
        for (Question question
                : questions) {

            if (question.getCategory()
                    == QuestionCategory.APTITUDE) {

                ordered.add(
                        question
                );
            }
        }

        /*
         * Next 15 Reasoning.
         */
        for (Question question
                : questions) {

            if (question.getCategory()
                    == QuestionCategory.REASONING) {

                ordered.add(
                        question
                );
            }
        }

        /*
         * Final 30 Technical.
         *
         * Group technical questions in the
         * same order as selected skills.
         */
        for (String skill
                : selectedSkills) {

            String normalizedSkill =
                    normalize(skill);

            for (Question question
                    : questions) {

                if (question.getCategory()
                        != QuestionCategory.TECHNICAL) {

                    continue;
                }

                if (question.getTechnicalSkill()
                        == null) {

                    continue;
                }

                if (normalize(
                        question.getTechnicalSkill()
                ).equals(normalizedSkill)) {

                    ordered.add(
                            question
                    );
                }
            }
        }

        if (ordered.size()
                != TOTAL_QUESTION_COUNT) {

            throw error(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not build the final 60-question test order."
            );
        }

        return ordered;
    }

    private String normalize(
            String value) {

        return value
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(
                        Locale.ROOT
                );
    }

    private ResponseStatusException error(
            HttpStatus status,
            String message) {

        return new ResponseStatusException(
                status,
                message
        );
    }
}