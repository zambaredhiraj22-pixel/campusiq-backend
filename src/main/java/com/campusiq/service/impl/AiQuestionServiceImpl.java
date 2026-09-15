package com.campusiq.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.campusiq.dto.AiGeneratedQuestionResponse;
import com.campusiq.dto.AiQuestionBulkApproveRequest;
import com.campusiq.dto.AiQuestionGenerateRequest;
import com.campusiq.dto.QuestionRequest;
import com.campusiq.dto.QuestionResponse;
import com.campusiq.entity.Question;
import com.campusiq.entity.Skill;
import com.campusiq.entity.StudentProfile;
import com.campusiq.enums.QuestionCategory;
import com.campusiq.enums.SkillStatus;
import com.campusiq.repository.QuestionRepository;
import com.campusiq.repository.SkillRepository;
import com.campusiq.repository.StudentProfileRepository;
import com.campusiq.service.AiQuestionProvider;
import com.campusiq.service.AiQuestionService;
import com.campusiq.service.QuestionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiQuestionServiceImpl
        implements AiQuestionService {

    private static final int APTITUDE_COUNT = 15;

    private static final int REASONING_COUNT = 15;

    private static final int TECHNICAL_COUNT = 30;

    private static final int TOTAL_QUESTION_COUNT = 60;

    private static final int MAX_GENERATION_ATTEMPTS = 4;

    private final StudentProfileRepository
            studentProfileRepository;

    private final SkillRepository skillRepository;

    private final QuestionRepository questionRepository;

    private final QuestionService questionService;

    private final ObjectProvider<AiQuestionProvider>
            aiQuestionProvider;

    @Override
    public List<AiGeneratedQuestionResponse>
            generateQuestions(
                    AiQuestionGenerateRequest request) {

        validateGenerateRequest(request);

        StudentProfile student =
                findStudent(
                        request.getStudentProfileId()
                );

        List<String> selectedVerifiedSkills =
                validateAndGetSelectedVerifiedSkills(
                        student,
                        request.getSelectedSkills()
                );

        AiQuestionProvider provider =
                aiQuestionProvider.getIfAvailable();

        if (provider == null) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI question provider is not configured"
            );
        }

        Set<String> usedQuestionTexts =
                loadExistingQuestionTexts();

        List<AiGeneratedQuestionResponse>
                generatedQuestions =
                new ArrayList<>();

        generatedQuestions.addAll(
                generateUniqueQuestions(
                        provider,
                        QuestionCategory.APTITUDE,
                        null,
                        APTITUDE_COUNT,
                        usedQuestionTexts
                )
        );

        generatedQuestions.addAll(
                generateUniqueQuestions(
                        provider,
                        QuestionCategory.REASONING,
                        null,
                        REASONING_COUNT,
                        usedQuestionTexts
                )
        );

        Map<String, Integer>
                technicalAllocation =
                createTechnicalAllocation(
                        selectedVerifiedSkills
                );

        for (Map.Entry<String, Integer> entry
                : technicalAllocation.entrySet()) {

            generatedQuestions.addAll(
                    generateUniqueQuestions(
                            provider,
                            QuestionCategory.TECHNICAL,
                            entry.getKey(),
                            entry.getValue(),
                            usedQuestionTexts
                    )
            );
        }

        if (generatedQuestions.size()
                != TOTAL_QUESTION_COUNT) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI generation did not produce exactly "
                            + TOTAL_QUESTION_COUNT
                            + " valid unique questions"
            );
        }

        return generatedQuestions;
    }

    /*
     * This method is intentionally added before it is declared
     * in AiQuestionService.
     *
     * In the next step we will add the same method to the
     * interface. This keeps the project compile-safe while
     * we update one complete file at a time.
     */
    @Transactional
    public List<QuestionResponse> approveQuestions(
            AiQuestionBulkApproveRequest request) {

        validateApprovalRequest(request);

        StudentProfile student =
                findStudent(
                        request.getStudentProfileId()
                );

        List<String> selectedVerifiedSkills =
                validateAndGetSelectedVerifiedSkills(
                        student,
                        request.getSelectedSkills()
                );

        Map<String, String>
                selectedSkillMap =
                createNormalizedSkillMap(
                        selectedVerifiedSkills
                );

        Map<String, Integer>
                expectedTechnicalAllocation =
                createTechnicalAllocation(
                        selectedVerifiedSkills
                );

        Map<String, Integer>
                actualTechnicalAllocation =
                new LinkedHashMap<>();

        for (String skill
                : selectedVerifiedSkills) {

            actualTechnicalAllocation.put(
                    skill,
                    0
            );
        }

        int aptitudeCount = 0;
        int reasoningCount = 0;
        int technicalCount = 0;

        Set<String> usedQuestionTexts =
                loadExistingQuestionTexts();

        for (QuestionRequest question
                : request.getQuestions()) {

            validateApprovedQuestion(
                    question
            );

            String normalizedQuestionText =
                    normalizeQuestionText(
                            question.getQuestionText()
                    );

            if (!usedQuestionTexts.add(
                    normalizedQuestionText)) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Duplicate question detected: "
                                + question
                                        .getQuestionText()
                                        .trim()
                );
            }

            if (question.getCategory()
                    == QuestionCategory.APTITUDE) {

                aptitudeCount++;

                question.setTechnicalSkill(null);

            } else if (question.getCategory()
                    == QuestionCategory.REASONING) {

                reasoningCount++;

                question.setTechnicalSkill(null);

            } else if (question.getCategory()
                    == QuestionCategory.TECHNICAL) {

                technicalCount++;

                String normalizedSkill =
                        normalizeSkill(
                                question
                                        .getTechnicalSkill()
                        );

                String verifiedSkillName =
                        selectedSkillMap.get(
                                normalizedSkill
                        );

                if (verifiedSkillName == null) {

                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Technical question uses skill '"
                                    + question
                                            .getTechnicalSkill()
                                            .trim()
                                    + "', which is not one of the "
                                    + "student's selected verified skills"
                    );
                }

                question.setTechnicalSkill(
                        verifiedSkillName
                );

                actualTechnicalAllocation.put(
                        verifiedSkillName,
                        actualTechnicalAllocation.get(
                                verifiedSkillName
                        ) + 1
                );
            }
        }

        validateSectionCounts(
                aptitudeCount,
                reasoningCount,
                technicalCount
        );

        validateTechnicalAllocation(
                expectedTechnicalAllocation,
                actualTechnicalAllocation
        );

        List<QuestionResponse> savedQuestions =
                new ArrayList<>();

        for (QuestionRequest question
                : request.getQuestions()) {

            QuestionResponse saved =
                    questionService.addQuestion(
                            question
                    );

            savedQuestions.add(saved);
        }

        return savedQuestions;
    }

    private StudentProfile findStudent(
            Long studentProfileId) {

        return studentProfileRepository
                .findById(studentProfileId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Student profile not found"
                        )
                );
    }

    private void validateGenerateRequest(
            AiQuestionGenerateRequest request) {

        if (request == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "AI question generation request is required"
            );
        }

        validateStudentAndSkills(
                request.getStudentProfileId(),
                request.getSelectedSkills()
        );
    }

    private void validateApprovalRequest(
            AiQuestionBulkApproveRequest request) {

        if (request == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "AI question approval request is required"
            );
        }

        validateStudentAndSkills(
                request.getStudentProfileId(),
                request.getSelectedSkills()
        );

        if (request.getQuestions() == null ||
                request.getQuestions().size()
                        != TOTAL_QUESTION_COUNT) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "AI mock test approval must contain exactly "
                            + TOTAL_QUESTION_COUNT
                            + " questions"
            );
        }
    }

    private void validateStudentAndSkills(
            Long studentProfileId,
            List<String> selectedSkills) {

        if (studentProfileId == null ||
                studentProfileId <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Student profile ID must be a positive number"
            );
        }

        if (selectedSkills == null ||
                selectedSkills.isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Select at least one verified technical skill"
            );
        }

        if (selectedSkills.size()
                > TECHNICAL_COUNT) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Maximum "
                            + TECHNICAL_COUNT
                            + " technical skills can be selected"
            );
        }
    }

    private List<String>
            validateAndGetSelectedVerifiedSkills(
                    StudentProfile student,
                    List<String> requestedSkills) {

        List<Skill> verifiedSkills =
                skillRepository
                        .findByStudentProfileAndStatus(
                                student,
                                SkillStatus.VERIFIED
                        );

        if (verifiedSkills.isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Student does not have any faculty-verified skills"
            );
        }

        Map<String, String>
                verifiedSkillMap =
                new LinkedHashMap<>();

        for (Skill skill
                : verifiedSkills) {

            if (skill.getSkillName() == null ||
                    skill.getSkillName().isBlank()) {

                continue;
            }

            String cleanSkillName =
                    skill.getSkillName().trim();

            verifiedSkillMap.putIfAbsent(
                    normalizeSkill(cleanSkillName),
                    cleanSkillName
            );
        }

        LinkedHashMap<String, String>
                selectedSkillMap =
                new LinkedHashMap<>();

        for (String requestedSkill
                : requestedSkills) {

            if (requestedSkill == null ||
                    requestedSkill.isBlank()) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Selected skill cannot be blank"
                );
            }

            String normalizedRequestedSkill =
                    normalizeSkill(
                            requestedSkill
                    );

            String verifiedSkillName =
                    verifiedSkillMap.get(
                            normalizedRequestedSkill
                    );

            if (verifiedSkillName == null) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Skill '"
                                + requestedSkill.trim()
                                + "' is not verified for this student"
                );
            }

            selectedSkillMap.putIfAbsent(
                    normalizedRequestedSkill,
                    verifiedSkillName
            );
        }

        if (selectedSkillMap.isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Select at least one verified technical skill"
            );
        }

        return new ArrayList<>(
                selectedSkillMap.values()
        );
    }

    private Map<String, String>
            createNormalizedSkillMap(
                    List<String> skills) {

        Map<String, String> map =
                new LinkedHashMap<>();

        for (String skill : skills) {

            map.put(
                    normalizeSkill(skill),
                    skill
            );
        }

        return map;
    }

    private Map<String, Integer>
            createTechnicalAllocation(
                    List<String> selectedSkills) {

        Map<String, Integer> allocation =
                new LinkedHashMap<>();

        int skillCount =
                selectedSkills.size();

        int baseCount =
                TECHNICAL_COUNT / skillCount;

        int remainder =
                TECHNICAL_COUNT % skillCount;

        for (int i = 0;
                i < selectedSkills.size();
                i++) {

            int questionCount =
                    baseCount;

            if (i < remainder) {
                questionCount++;
            }

            allocation.put(
                    selectedSkills.get(i),
                    questionCount
            );
        }

        return allocation;
    }

    private void validateSectionCounts(
            int aptitudeCount,
            int reasoningCount,
            int technicalCount) {

        if (aptitudeCount != APTITUDE_COUNT) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "AI mock test must contain exactly "
                            + APTITUDE_COUNT
                            + " aptitude questions"
            );
        }

        if (reasoningCount != REASONING_COUNT) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "AI mock test must contain exactly "
                            + REASONING_COUNT
                            + " reasoning questions"
            );
        }

        if (technicalCount != TECHNICAL_COUNT) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "AI mock test must contain exactly "
                            + TECHNICAL_COUNT
                            + " technical questions"
            );
        }
    }

    private void validateTechnicalAllocation(
            Map<String, Integer> expected,
            Map<String, Integer> actual) {

        for (Map.Entry<String, Integer> entry
                : expected.entrySet()) {

            String skill =
                    entry.getKey();

            int expectedCount =
                    entry.getValue();

            int actualCount =
                    actual.getOrDefault(
                            skill,
                            0
                    );

            if (actualCount != expectedCount) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Technical questions for "
                                + skill
                                + " must be exactly "
                                + expectedCount
                                + ", but received "
                                + actualCount
                );
            }
        }
    }

    private void validateApprovedQuestion(
            QuestionRequest question) {

        if (question == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Approved question cannot be null"
            );
        }

        if (isBlank(question.getQuestionText()) ||
                isBlank(question.getOptionA()) ||
                isBlank(question.getOptionB()) ||
                isBlank(question.getOptionC()) ||
                isBlank(question.getOptionD())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Question text and all four options are required"
            );
        }

        if (question.getCategory() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Question category is required"
            );
        }

        if (isBlank(question.getCorrectOption()) ||
                !question.getCorrectOption()
                        .trim()
                        .matches("(?i)[A-D]")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Correct option must be A, B, C or D"
            );
        }

        if (question.getCategory()
                == QuestionCategory.TECHNICAL &&
                isBlank(
                        question.getTechnicalSkill()
                )) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Technical skill is required for technical questions"
            );
        }

        Set<String> normalizedOptions =
                new HashSet<>();

        normalizedOptions.add(
                normalizeOption(
                        question.getOptionA()
                )
        );

        normalizedOptions.add(
                normalizeOption(
                        question.getOptionB()
                )
        );

        normalizedOptions.add(
                normalizeOption(
                        question.getOptionC()
                )
        );

        normalizedOptions.add(
                normalizeOption(
                        question.getOptionD()
                )
        );

        if (normalizedOptions.size() != 4) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "All four answer options must be different"
            );
        }
    }

    private List<AiGeneratedQuestionResponse>
            generateUniqueQuestions(
                    AiQuestionProvider provider,
                    QuestionCategory category,
                    String technicalSkill,
                    int requiredCount,
                    Set<String> usedQuestionTexts) {

        List<AiGeneratedQuestionResponse>
                acceptedQuestions =
                new ArrayList<>();

        int attempts = 0;

        while (acceptedQuestions.size()
                < requiredCount &&
                attempts
                        < MAX_GENERATION_ATTEMPTS) {

            attempts++;

            int remaining =
                    requiredCount
                            - acceptedQuestions.size();

            List<AiGeneratedQuestionResponse>
                    aiQuestions =
                    provider.generateQuestions(
                            category,
                            technicalSkill,
                            remaining
                    );

            if (aiQuestions == null ||
                    aiQuestions.isEmpty()) {

                continue;
            }

            for (AiGeneratedQuestionResponse question
                    : aiQuestions) {

                if (acceptedQuestions.size()
                        >= requiredCount) {

                    break;
                }

                if (!isValidGeneratedQuestion(
                        question)) {

                    continue;
                }

                cleanGeneratedQuestion(
                        question,
                        category,
                        technicalSkill
                );

                String normalizedText =
                        normalizeQuestionText(
                                question
                                        .getQuestionText()
                        );

                if (usedQuestionTexts.contains(
                        normalizedText)) {

                    continue;
                }

                usedQuestionTexts.add(
                        normalizedText
                );

                acceptedQuestions.add(
                        question
                );
            }
        }

        if (acceptedQuestions.size()
                != requiredCount) {

            String sectionName =
                    category
                            == QuestionCategory.TECHNICAL
                            ? "technical skill "
                                    + technicalSkill
                            : category.name()
                                    .toLowerCase(
                                            Locale.ROOT
                                    );

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI could not generate enough unique questions for "
                            + sectionName
            );
        }

        return acceptedQuestions;
    }

    private boolean isValidGeneratedQuestion(
            AiGeneratedQuestionResponse question) {

        if (question == null) {
            return false;
        }

        if (isBlank(question.getQuestionText()) ||
                isBlank(question.getOptionA()) ||
                isBlank(question.getOptionB()) ||
                isBlank(question.getOptionC()) ||
                isBlank(question.getOptionD()) ||
                isBlank(question.getCorrectOption())) {

            return false;
        }

        String correctOption =
                question.getCorrectOption()
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (!correctOption.matches("[A-D]")) {
            return false;
        }

        Set<String> options =
                new HashSet<>();

        options.add(
                normalizeOption(
                        question.getOptionA()
                )
        );

        options.add(
                normalizeOption(
                        question.getOptionB()
                )
        );

        options.add(
                normalizeOption(
                        question.getOptionC()
                )
        );

        options.add(
                normalizeOption(
                        question.getOptionD()
                )
        );

        return options.size() == 4;
    }

    private void cleanGeneratedQuestion(
            AiGeneratedQuestionResponse question,
            QuestionCategory category,
            String technicalSkill) {

        question.setQuestionText(
                question.getQuestionText().trim()
        );

        question.setOptionA(
                question.getOptionA().trim()
        );

        question.setOptionB(
                question.getOptionB().trim()
        );

        question.setOptionC(
                question.getOptionC().trim()
        );

        question.setOptionD(
                question.getOptionD().trim()
        );

        question.setCorrectOption(
                question.getCorrectOption()
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        )
        );

        question.setCategory(category);

        if (category
                == QuestionCategory.TECHNICAL) {

            question.setTechnicalSkill(
                    technicalSkill
            );

        } else {

            question.setTechnicalSkill(null);
        }
    }

    private Set<String>
            loadExistingQuestionTexts() {

        Set<String> questionTexts =
                new HashSet<>();

        List<Question> existingQuestions =
                questionRepository.findAll();

        for (Question question
                : existingQuestions) {

            if (question.getQuestionText()
                    == null ||
                    question.getQuestionText()
                            .isBlank()) {

                continue;
            }

            questionTexts.add(
                    normalizeQuestionText(
                            question
                                    .getQuestionText()
                    )
            );
        }

        return questionTexts;
    }

    private String normalizeQuestionText(
            String value) {

        return value
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(
                        Locale.ROOT
                );
    }

    private String normalizeOption(
            String value) {

        return value
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(
                        Locale.ROOT
                );
    }

    private String normalizeSkill(
            String value) {

        return value
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(
                        Locale.ROOT
                );
    }

    private boolean isBlank(
            String value) {

        return value == null ||
                value.isBlank();
    }
}