package com.campusiq.service.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import com.campusiq.dto.MockTestRequest;
import com.campusiq.dto.MockTestResponse;
import com.campusiq.dto.StartMockTestRequest;
import com.campusiq.dto.StartMockTestResponse;
import com.campusiq.dto.StudentAnswerRequest;
import com.campusiq.dto.StudentQuestionResponse;
import com.campusiq.dto.SubmitMockTestRequest;
import com.campusiq.dto.SubmitMockTestResponse;

import com.campusiq.entity.MockTest;
import com.campusiq.entity.MockTestQuestion;
import com.campusiq.entity.MockTestResult;
import com.campusiq.entity.Question;
import com.campusiq.entity.StudentMockTestAssignment;
import com.campusiq.entity.StudentProfile;
import com.campusiq.entity.TestAttempt;
import com.campusiq.entity.TestAttemptQuestion;
import com.campusiq.entity.User;

import com.campusiq.enums.QuestionCategory;
import com.campusiq.enums.Role;
import com.campusiq.enums.SkillStatus;

import com.campusiq.repository.MockTestQuestionRepository;
import com.campusiq.repository.MockTestRepository;
import com.campusiq.repository.MockTestResultRepository;
import com.campusiq.repository.QuestionRepository;
import com.campusiq.repository.SkillRepository;
import com.campusiq.repository.StudentMockTestAssignmentRepository;
import com.campusiq.repository.StudentProfileRepository;
import com.campusiq.repository.TestAttemptQuestionRepository;
import com.campusiq.repository.TestAttemptRepository;
import com.campusiq.repository.UserRepository;

import com.campusiq.service.EmailService;
import com.campusiq.service.MockTestService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(isolation = Isolation.READ_COMMITTED)
public class MockTestServiceImpl implements MockTestService {

    private static final int AUTO_SUBMIT_WARNING_COUNT = 3;

    /*
     * Fixed structure for AI Personalized Mock Test.
     */
    private static final int AI_APTITUDE_COUNT = 15;
    private static final int AI_REASONING_COUNT = 15;
    private static final int AI_TECHNICAL_COUNT = 30;
    private static final int AI_TOTAL_COUNT = 60;

    private final MockTestRepository mockTestRepository;

    private final UserRepository userRepository;

    private final StudentProfileRepository studentProfileRepository;

    private final SkillRepository skillRepository;

    private final QuestionRepository questionRepository;

    private final MockTestResultRepository mockTestResultRepository;

    private final TestAttemptRepository testAttemptRepository;

    private final TestAttemptQuestionRepository
            testAttemptQuestionRepository;

    private final MockTestQuestionRepository
            mockTestQuestionRepository;

    private final StudentMockTestAssignmentRepository
            studentMockTestAssignmentRepository;

    private final EmailService emailService;

    /*
     * =====================================================
     * FACULTY - MANUAL MOCK TEST CREATION
     * =====================================================
     *
     * Existing manual flow remains unchanged.
     */
    @Override
    public MockTestResponse createMockTest(
            MockTestRequest request) {

        if (request == null ||
                request.getTitle() == null ||
                request.getTitle().isBlank()) {

            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Test title is required."
            );
        }

        validateTestConfiguration(
                request.getAptitudeQuestionCount(),
                request.getReasoningQuestionCount(),
                request.getTechnicalQuestionCount(),
                request.getDurationMinutes()
        );

        MockTest mockTest =
                new MockTest();

        mockTest.setTitle(
                request.getTitle().trim()
        );

        mockTest.setAptitudeQuestionCount(
                request.getAptitudeQuestionCount()
        );

        mockTest.setReasoningQuestionCount(
                request.getReasoningQuestionCount()
        );

        mockTest.setTechnicalQuestionCount(
                request.getTechnicalQuestionCount()
        );

        mockTest.setDurationMinutes(
                request.getDurationMinutes()
        );

        mockTest.setActive(
                request.isActive()
        );

        /*
         * MockTest keeps existing default
         * pass percentage = 65.
         */
        MockTest saved =
                mockTestRepository.save(
                        mockTest
                );

        return mapToResponse(saved);
    }

    /*
     * =====================================================
     * FACULTY - ALL MOCK TESTS
     * =====================================================
     *
     * Faculty may see Manual + AI tests.
     */
    @Override
    @Transactional(readOnly = true)
    public List<MockTestResponse> getAllMockTests() {

        List<MockTestResponse> responses =
                new ArrayList<>();

        for (MockTest mockTest
                : mockTestRepository.findAll()) {

            responses.add(
                    mapToResponse(mockTest)
            );
        }

        return responses;
    }

    /*
     * =====================================================
     * STUDENT - AVAILABLE MOCK TESTS
     * =====================================================
     *
     * Manual tests:
     * → visible to students as before.
     *
     * AI Personalized tests:
     * → visible ONLY if assigned to this student.
     *
     * This method will be exposed through
     * MockTestService interface in the next file.
     */
    @Transactional(readOnly = true)
    public List<MockTestResponse>
            getAvailableMockTests(
                    String username) {

        StudentProfile student =
                getStudent(username);

        List<MockTestResponse> responses =
                new ArrayList<>();

        /*
         * First add personalized assignments.
         */
        List<StudentMockTestAssignment>
                assignments =
                studentMockTestAssignmentRepository
                        .findByStudentProfileIdAndActiveTrueOrderByAssignedAtDesc(
                                student.getId()
                        );

        Set<Long> addedTestIds =
                new HashSet<>();

        for (StudentMockTestAssignment assignment
                : assignments) {

            MockTest mockTest =
                    assignment.getMockTest();

            if (mockTest == null ||
                    !mockTest.isActive()) {

                continue;
            }

            /*
             * Assignment must point to a test
             * having exact question mappings.
             */
            if (!isPersonalizedMockTest(
                    mockTest.getId())) {

                continue;
            }

            responses.add(
                    mapToAssignedPersonalizedResponse(
                            assignment
                    )
            );

            addedTestIds.add(
                    mockTest.getId()
            );
        }

        /*
         * Add normal active Manual Mock Tests.
         *
         * Any test having MockTestQuestion mappings
         * is considered personalized and therefore
         * must NOT be globally visible.
         */
        for (MockTest mockTest
                : mockTestRepository.findAll()) {

            if (!mockTest.isActive()) {
                continue;
            }

            if (addedTestIds.contains(
                    mockTest.getId())) {

                continue;
            }

            if (isPersonalizedMockTest(
                    mockTest.getId())) {

                continue;
            }

            responses.add(
                    mapToResponse(mockTest)
            );
        }

        return responses;
    }

    /*
     * =====================================================
     * STUDENT - START MOCK TEST
     * =====================================================
     *
     * Automatically determines:
     *
     * MANUAL TEST
     * → selectedSkill required
     * → verified skill check
     * → random Question Bank selection
     *
     * AI PERSONALIZED TEST
     * → selectedSkill NOT required
     * → assignment required
     * → exact 60 mapped questions
     */
    @Override
    public StartMockTestResponse startMockTest(
            String username,
            StartMockTestRequest request) {

        if (request == null) {

            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Start request is required."
            );
        }

        requireId(
                request.getMockTestId(),
                "Mock test ID"
        );

        StudentProfile student =
                getStudent(username);

        MockTest mockTest =
                mockTestRepository
                        .findById(
                                request.getMockTestId()
                        )
                        .orElseThrow(() ->
                                error(
                                        HttpStatus.NOT_FOUND,
                                        "Mock test not found."
                                )
                        );

        if (!mockTest.isActive()) {

            throw error(
                    HttpStatus.CONFLICT,
                    "Mock test is not active."
            );
        }

        validateTestConfiguration(
                mockTest.getAptitudeQuestionCount(),
                mockTest.getReasoningQuestionCount(),
                mockTest.getTechnicalQuestionCount(),
                mockTest.getDurationMinutes()
        );

        if (mockTest.getPassPercentage() < 0 ||
                mockTest.getPassPercentage() > 100) {

            throw error(
                    HttpStatus.CONFLICT,
                    "Invalid test pass percentage."
            );
        }

        boolean personalized =
                isPersonalizedMockTest(
                        mockTest.getId()
                );

        List<Question> chosen;

        String attemptSkillLabel;

        if (personalized) {

            /*
             * ================================
             * AI PERSONALIZED FLOW
             * ================================
             */

            verifyPersonalizedAssignment(
                    student,
                    mockTest
            );

            List<MockTestQuestion> mappings =
                    loadAndValidatePersonalizedMappings(
                            mockTest
                    );

            chosen =
                    mappings.stream()
                            .map(
                                    MockTestQuestion::getQuestion
                            )
                            .toList();

            /*
             * TestAttempt has an existing
             * selectedSkill String field.
             *
             * For AI test we store a summary
             * of all technical skills so old
             * TestAttempt/Proctoring code remains
             * compatible.
             */
            attemptSkillLabel =
                    buildPersonalizedSkillLabel(
                            mappings
                    );

        } else {

            /*
             * ================================
             * EXISTING MANUAL FLOW
             * ================================
             */

            if (request.getSelectedSkill() == null ||
                    request.getSelectedSkill()
                            .isBlank()) {

                throw error(
                        HttpStatus.BAD_REQUEST,
                        "Select a verified technical skill."
                );
            }

            String selectedSkill =
                    request.getSelectedSkill()
                            .trim();

            boolean verified =
                    skillRepository
                            .existsByStudentProfileAndSkillNameIgnoreCaseAndStatus(
                                    student,
                                    selectedSkill,
                                    SkillStatus.VERIFIED
                            );

            if (!verified) {

                throw error(
                        HttpStatus.FORBIDDEN,
                        "Selected skill is not verified."
                );
            }

            chosen =
                    selectManualQuestions(
                            mockTest,
                            selectedSkill
                    );

            attemptSkillLabel =
                    selectedSkill;
        }

        /*
         * Both flows now use the same secure
         * TestAttempt + snapshot architecture.
         */
        return createAttempt(
                student,
                mockTest,
                chosen,
                attemptSkillLabel
        );
    }

    /*
     * =====================================================
     * CREATE SECURE ATTEMPT
     * =====================================================
     */
    private StartMockTestResponse createAttempt(
            StudentProfile student,
            MockTest mockTest,
            List<Question> questions,
            String skillLabel) {

        if (questions == null ||
                questions.isEmpty()) {

            throw error(
                    HttpStatus.CONFLICT,
                    "No questions are available for this mock test."
            );
        }

        TestAttempt attempt =
                new TestAttempt();

        attempt.setStudentProfile(
                student
        );

        attempt.setMockTest(
                mockTest
        );

        attempt.setSelectedSkill(
                skillLabel
        );

        attempt.setTestTitle(
                mockTest.getTitle()
        );

        attempt.setDurationMinutes(
                mockTest.getDurationMinutes()
        );

        attempt.setTotalQuestions(
                questions.size()
        );

        attempt.setPassPercentage(
                mockTest.getPassPercentage()
        );

        attempt.setStartedAt(
                LocalDateTime.now()
        );

        attempt.setExpiresAt(
                attempt.getStartedAt()
                        .plusMinutes(
                                mockTest.getDurationMinutes()
                        )
        );

        attempt.setWarningCount(0);

        attempt.setCompleted(false);

        attempt.setAutoSubmitted(false);

        attempt =
                testAttemptRepository.save(
                        attempt
                );

        /*
         * Snapshot protects the test from
         * later Question Bank modifications.
         */
        List<TestAttemptQuestion> snapshots =
                new ArrayList<>();

        for (int index = 0;
                index < questions.size();
                index++) {

            snapshots.add(
                    snapshot(
                            attempt,
                            questions.get(index),
                            index + 1
                    )
            );
        }

        testAttemptQuestionRepository
                .saveAll(snapshots);

        return mapToStartResponse(
                attempt,
                snapshots
        );
    }

    /*
     * =====================================================
     * EXISTING MANUAL QUESTION SELECTION
     * =====================================================
     */
    private List<Question> selectManualQuestions(
            MockTest mockTest,
            String selectedSkill) {

        List<Question> chosen =
                new ArrayList<>();

        chosen.addAll(
                selectQuestions(
                        questionRepository
                                .findByCategory(
                                        QuestionCategory.APTITUDE
                                ),
                        mockTest.getAptitudeQuestionCount(),
                        "aptitude"
                )
        );

        chosen.addAll(
                selectQuestions(
                        questionRepository
                                .findByCategory(
                                        QuestionCategory.REASONING
                                ),
                        mockTest.getReasoningQuestionCount(),
                        "reasoning"
                )
        );

        chosen.addAll(
                selectQuestions(
                        questionRepository
                                .findByCategoryAndTechnicalSkillIgnoreCase(
                                        QuestionCategory.TECHNICAL,
                                        selectedSkill
                                ),
                        mockTest.getTechnicalQuestionCount(),
                        selectedSkill
                )
        );

        /*
         * Existing manual tests remain randomized.
         */
        Collections.shuffle(chosen);

        return chosen;
    }

    /*
     * =====================================================
     * AI PERSONALIZED ASSIGNMENT SECURITY
     * =====================================================
     */
    private void verifyPersonalizedAssignment(
            StudentProfile student,
            MockTest mockTest) {

        boolean assigned =
                studentMockTestAssignmentRepository
                        .existsByStudentProfileIdAndMockTestIdAndActiveTrue(
                                student.getId(),
                                mockTest.getId()
                        );

        if (!assigned) {

            throw error(
                    HttpStatus.FORBIDDEN,
                    "This personalized mock test is not assigned to you."
            );
        }
    }

    /*
     * =====================================================
     * LOAD + DEFENSIVELY VALIDATE AI MAPPINGS
     * =====================================================
     */
    private List<MockTestQuestion>
            loadAndValidatePersonalizedMappings(
                    MockTest mockTest) {

        List<MockTestQuestion> mappings =
                mockTestQuestionRepository
                        .findByMockTestIdOrderByQuestionOrderAsc(
                                mockTest.getId()
                        );

        if (mappings.size()
                != AI_TOTAL_COUNT) {

            throw error(
                    HttpStatus.CONFLICT,
                    "Personalized AI mock test must contain exactly 60 mapped questions."
            );
        }

        /*
         * AI Mock Test configuration itself
         * must remain 15 + 15 + 30.
         */
        if (mockTest.getAptitudeQuestionCount()
                != AI_APTITUDE_COUNT ||
                mockTest.getReasoningQuestionCount()
                        != AI_REASONING_COUNT ||
                mockTest.getTechnicalQuestionCount()
                        != AI_TECHNICAL_COUNT) {

            throw error(
                    HttpStatus.CONFLICT,
                    "Invalid personalized AI mock test configuration."
            );
        }

        int aptitude = 0;
        int reasoning = 0;
        int technical = 0;

        Set<Long> questionIds =
                new HashSet<>();

        int expectedOrder = 1;

        for (MockTestQuestion mapping
                : mappings) {

            if (mapping.getQuestion() == null) {

                throw error(
                        HttpStatus.CONFLICT,
                        "A personalized test question mapping is incomplete."
                );
            }

            if (mapping.getQuestionOrder() == null ||
                    mapping.getQuestionOrder()
                            != expectedOrder) {

                throw error(
                        HttpStatus.CONFLICT,
                        "Personalized mock test question order is invalid."
                );
            }

            Question question =
                    mapping.getQuestion();

            if (question.getId() == null ||
                    !questionIds.add(
                            question.getId()
                    )) {

                throw error(
                        HttpStatus.CONFLICT,
                        "Duplicate question found in personalized mock test."
                );
            }

            if (question.getCategory()
                    == QuestionCategory.APTITUDE) {

                aptitude++;

            } else if (question.getCategory()
                    == QuestionCategory.REASONING) {

                reasoning++;

            } else if (question.getCategory()
                    == QuestionCategory.TECHNICAL) {

                technical++;

                if (question.getTechnicalSkill()
                        == null ||
                        question.getTechnicalSkill()
                                .isBlank()) {

                    throw error(
                            HttpStatus.CONFLICT,
                            "A technical AI question has no technical skill."
                    );
                }

            } else {

                throw error(
                        HttpStatus.CONFLICT,
                        "Unsupported question category exists in personalized mock test."
                );
            }

            expectedOrder++;
        }

        if (aptitude != AI_APTITUDE_COUNT ||
                reasoning != AI_REASONING_COUNT ||
                technical != AI_TECHNICAL_COUNT) {

            throw error(
                    HttpStatus.CONFLICT,
                    "Personalized mock test must contain 15 aptitude, 15 reasoning and 30 technical questions."
            );
        }

        return mappings;
    }

    /*
     * =====================================================
     * BUILD MULTI-SKILL LABEL
     * =====================================================
     *
     * Example:
     * Java, Python, Spring Boot
     *
     * Keeps old TestAttempt.selectedSkill
     * and Proctoring metadata compatible.
     */
    private String buildPersonalizedSkillLabel(
            List<MockTestQuestion> mappings) {

        LinkedHashSet<String> skills =
                new LinkedHashSet<>();

        for (MockTestQuestion mapping
                : mappings) {

            Question question =
                    mapping.getQuestion();

            if (question.getCategory()
                    != QuestionCategory.TECHNICAL) {

                continue;
            }

            String skill =
                    question.getTechnicalSkill();

            if (skill != null &&
                    !skill.isBlank()) {

                skills.add(
                        skill.trim()
                );
            }
        }

        if (skills.isEmpty()) {

            throw error(
                    HttpStatus.CONFLICT,
                    "Personalized test has no technical skills."
            );
        }

        String label =
                String.join(
                        ", ",
                        skills
                );

        /*
         * Typical selected skills are only
         * a few values, but keep this safe
         * for the existing VARCHAR column.
         */
        if (label.length() > 250) {

            label =
                    label.substring(
                            0,
                            250
                    );
        }

        return label;
    }

    /*
     * =====================================================
     * SUBMIT TEST
     * =====================================================
     */
    @Override
    public SubmitMockTestResponse submitMockTest(
            String username,
            SubmitMockTestRequest request) {

        if (request == null) {

            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Submit request is required."
            );
        }

        requireId(
                request.getMockTestId(),
                "Mock test ID"
        );

        TestAttempt attempt =
                getOwnedAttemptForUpdate(
                        username,
                        request.getTestAttemptId()
                );

        if (!Objects.equals(
                attempt.getMockTest().getId(),
                request.getMockTestId())) {

            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Mock test does not match this attempt."
            );
        }

        requireAttemptMetadata(
                attempt
        );

        /*
         * Retry-safe result.
         */
        if (attempt.getMockTestResult()
                != null) {

            return mapToSubmitResponse(
                    attempt
            );
        }

        List<TestAttemptQuestion> snapshots =
                getSnapshots(attempt);

        if (attempt.isCompleted() ||
                mustAutoSubmit(attempt)) {

            return finishAttempt(
                    attempt,
                    snapshots,
                    attempt.isAutoSubmitted()
                            || mustAutoSubmit(
                                    attempt
                            )
            );
        }

        Map<Long, String> updates =
                validateAnswers(
                        request.getAnswers(),
                        snapshots
                );

        /*
         * Deadline is checked again before
         * accepting submitted answers.
         */
        if (mustAutoSubmit(attempt)) {

            return finishAttempt(
                    attempt,
                    snapshots,
                    true
            );
        }

        for (TestAttemptQuestion question
                : snapshots) {

            if (updates.containsKey(
                    question.getQuestionId())) {

                question.setSelectedOption(
                        updates.get(
                                question.getQuestionId()
                        )
                );
            }
        }

        testAttemptQuestionRepository
                .saveAll(snapshots);

        return finishAttempt(
                attempt,
                snapshots,
                false
        );
    }

    /*
     * =====================================================
     * RESTORE EXISTING ATTEMPT
     * =====================================================
     */
    @Override
    public StartMockTestResponse getAttempt(
            String username,
            Long testAttemptId) {

        TestAttempt attempt =
                getOwnedAttemptForUpdate(
                        username,
                        testAttemptId
                );

        requireAttemptMetadata(
                attempt
        );

        requireOpenAttempt(
                attempt
        );

        return mapToStartResponse(
                attempt,
                getSnapshots(attempt)
        );
    }

    /*
     * =====================================================
     * SAVE ONE ANSWER
     * =====================================================
     */
    @Override
    public StudentQuestionResponse saveAnswer(
            String username,
            Long testAttemptId,
            StudentAnswerRequest request) {

        TestAttempt attempt =
                getOwnedAttemptForUpdate(
                        username,
                        testAttemptId
                );

        requireAttemptMetadata(
                attempt
        );

        requireOpenAttempt(
                attempt
        );

        if (request == null) {

            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Answer request is required."
            );
        }

        requireId(
                request.getQuestionId(),
                "Question ID"
        );

        TestAttemptQuestion question =
                testAttemptQuestionRepository
                        .findByTestAttempt_IdAndQuestionId(
                                testAttemptId,
                                request.getQuestionId()
                        )
                        .orElseThrow(() ->
                                error(
                                        HttpStatus.BAD_REQUEST,
                                        "This question was not assigned to this attempt."
                                )
                        );

        String option =
                normalizeSelectedOption(
                        request.getSelectedOption()
                );

        /*
         * Recheck because time may have
         * expired during request processing.
         */
        requireOpenAttempt(
                attempt
        );

        question.setSelectedOption(
                option
        );

        testAttemptQuestionRepository
                .save(question);

        return mapToStudentQuestion(
                question
        );
    }

    /*
     * =====================================================
     * GET / FINALIZE RESULT
     * =====================================================
     */
    @Override
    public SubmitMockTestResponse getResult(
            String username,
            Long testAttemptId) {

        TestAttempt attempt =
                getOwnedAttemptForUpdate(
                        username,
                        testAttemptId
                );

        requireAttemptMetadata(
                attempt
        );

        if (attempt.getMockTestResult()
                != null) {

            return mapToSubmitResponse(
                    attempt
            );
        }

        if (!attempt.isCompleted() &&
                !mustAutoSubmit(attempt)) {

            throw error(
                    HttpStatus.CONFLICT,
                    "This attempt is still in progress."
            );
        }

        return finishAttempt(
                attempt,
                getSnapshots(attempt),
                attempt.isAutoSubmitted()
                        || mustAutoSubmit(
                                attempt
                        )
        );
    }

    /*
     * =====================================================
     * INTERNAL TIMEOUT SCHEDULER
     * =====================================================
     */
    @Override
    public boolean finalizeExpiredAttempt(
            Long testAttemptId) {

        requireId(
                testAttemptId,
                "Test attempt ID"
        );

        TestAttempt attempt =
                testAttemptRepository
                        .findByIdForUpdate(
                                testAttemptId
                        )
                        .orElse(null);

        if (attempt == null ||
                attempt.getMockTestResult()
                        != null ||
                attempt.getExpiresAt()
                        == null ||
                !isExpired(attempt)) {

            return false;
        }

        requireAttemptMetadata(
                attempt
        );

        finishAttempt(
                attempt,
                getSnapshots(attempt),
                true
        );

        return true;
    }

    /*
     * =====================================================
     * STUDENT SECURITY
     * =====================================================
     */
    private StudentProfile getStudent(
            String username) {

        if (username == null ||
                username.isBlank()) {

            throw error(
                    HttpStatus.UNAUTHORIZED,
                    "Login is required."
            );
        }

        User user =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() ->
                                error(
                                        HttpStatus.UNAUTHORIZED,
                                        "User not found."
                                )
                        );

        if (user.getRole()
                != Role.STUDENT) {

            throw error(
                    HttpStatus.FORBIDDEN,
                    "Only students can take mock tests."
            );
        }

        return studentProfileRepository
                .findByUser(user)
                .orElseThrow(() ->
                        error(
                                HttpStatus.NOT_FOUND,
                                "Student profile not found."
                        )
                );
    }

    /*
     * =====================================================
     * ATTEMPT OWNERSHIP
     * =====================================================
     */
    private TestAttempt getOwnedAttemptForUpdate(
            String username,
            Long testAttemptId) {

        requireId(
                testAttemptId,
                "Test attempt ID"
        );

        StudentProfile student =
                getStudent(username);

        TestAttempt attempt =
                testAttemptRepository
                        .findByIdForUpdate(
                                testAttemptId
                        )
                        .orElseThrow(() ->
                                error(
                                        HttpStatus.NOT_FOUND,
                                        "Test attempt not found."
                                )
                        );

        if (!Objects.equals(
                attempt.getStudentProfile().getId(),
                student.getId())) {

            throw error(
                    HttpStatus.FORBIDDEN,
                    "You cannot access another student's attempt."
            );
        }

        return attempt;
    }

    /*
     * =====================================================
     * ATTEMPT METADATA VALIDATION
     * =====================================================
     */
    private void requireAttemptMetadata(
            TestAttempt attempt) {

        if (attempt.getTotalQuestions()
                        == null ||
                attempt.getTotalQuestions()
                        <= 0 ||
                attempt.getPassPercentage()
                        == null ||
                attempt.getPassPercentage()
                        < 0 ||
                attempt.getPassPercentage()
                        > 100 ||
                attempt.getStartedAt()
                        == null ||
                attempt.getExpiresAt()
                        == null ||
                attempt.getDurationMinutes()
                        == null ||
                attempt.getDurationMinutes()
                        <= 0 ||
                attempt.getSelectedSkill()
                        == null ||
                attempt.getTestTitle()
                        == null) {

            throw error(
                    HttpStatus.CONFLICT,
                    "This older attempt has no saved test details. Start a new mock-test attempt."
            );
        }
    }

    /*
     * =====================================================
     * LOAD SAVED QUESTION SNAPSHOTS
     * =====================================================
     */
    private List<TestAttemptQuestion>
            getSnapshots(
                    TestAttempt attempt) {

        List<TestAttemptQuestion> questions =
                testAttemptQuestionRepository
                        .findByTestAttempt_IdOrderByQuestionNumberAsc(
                                attempt.getId()
                        );

        if (questions.size()
                != attempt.getTotalQuestions()) {

            throw error(
                    HttpStatus.CONFLICT,
                    "Saved attempt questions are incomplete."
            );
        }

        return questions;
    }

    /*
     * =====================================================
     * OPEN / CLOSED CHECK
     * =====================================================
     */
    private void requireOpenAttempt(
            TestAttempt attempt) {

        if (attempt.isCompleted() ||
                attempt.getMockTestResult()
                        != null ||
                mustAutoSubmit(attempt)) {

            throw error(
                    HttpStatus.CONFLICT,
                    "This attempt is closed or its time has expired. Fetch the result."
            );
        }
    }

    private boolean isExpired(
            TestAttempt attempt) {

        return !LocalDateTime.now()
                .isBefore(
                        attempt.getExpiresAt()
                );
    }

    private boolean mustAutoSubmit(
            TestAttempt attempt) {

        return attempt.isAutoSubmitted()
                ||
                attempt.getWarningCount()
                        >= AUTO_SUBMIT_WARNING_COUNT
                ||
                isExpired(attempt);
    }

    /*
     * =====================================================
     * ANSWER VALIDATION
     * =====================================================
     */
    private Map<Long, String> validateAnswers(
            List<StudentAnswerRequest> answers,
            List<TestAttemptQuestion> snapshots) {

        Map<Long, String> updates =
                new HashMap<>();

        /*
         * Omitted answers keep previously
         * saved server values.
         */
        if (answers == null) {

            return updates;
        }

        if (answers.size()
                > snapshots.size()) {

            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Too many answers supplied."
            );
        }

        Set<Long> assignedIds =
                new HashSet<>();

        for (TestAttemptQuestion question
                : snapshots) {

            assignedIds.add(
                    question.getQuestionId()
            );
        }

        for (StudentAnswerRequest answer
                : answers) {

            if (answer == null ||
                    answer.getQuestionId()
                            == null ||
                    !assignedIds.contains(
                            answer.getQuestionId()
                    )) {

                throw error(
                        HttpStatus.BAD_REQUEST,
                        "Every answer must belong to a question assigned to this attempt."
                );
            }

            if (updates.containsKey(
                    answer.getQuestionId())) {

                throw error(
                        HttpStatus.BAD_REQUEST,
                        "Duplicate question answer found."
                );
            }

            updates.put(
                    answer.getQuestionId(),
                    normalizeSelectedOption(
                            answer.getSelectedOption()
                    )
            );
        }

        return updates;
    }

    private String normalizeSelectedOption(
            String option) {

        if (option == null) {

            return null;
        }

        String normalized =
                option.trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (!normalized.matches(
                "[ABCD]")) {

            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Selected option must be A, B, C or D, or null."
            );
        }

        return normalized;
    }

    /*
     * =====================================================
     * FINAL RESULT CALCULATION
     * =====================================================
     */
    private SubmitMockTestResponse finishAttempt(
            TestAttempt attempt,
            List<TestAttemptQuestion> questions,
            boolean automatic) {

        /*
         * Retry safe.
         */
        if (attempt.getMockTestResult()
                != null) {

            return mapToSubmitResponse(
                    attempt
            );
        }

        int total =
                attempt.getTotalQuestions();

        int correct = 0;

        for (TestAttemptQuestion question
                : questions) {

            if (question.getSelectedOption()
                        != null &&
                    question.getSelectedOption()
                            .equals(
                                    question.getCorrectOption()
                            )) {

                correct++;
            }
        }

        double percentage =
                Math.round(
                        correct
                                * 10000.0
                                / total
                ) / 100.0;

        /*
         * Pass/fail uses actual score instead
         * of rounded percentage.
         */
        boolean passed =
                (long) correct * 100
                        >=
                (long) attempt
                        .getPassPercentage()
                        * total;

        LocalDateTime submittedAt =
                attempt.getSubmittedAt()
                        == null
                        ? LocalDateTime.now()
                        : attempt.getSubmittedAt();

        MockTestResult result =
                new MockTestResult();

        result.setStudentProfile(
                attempt.getStudentProfile()
        );

        result.setMockTest(
                attempt.getMockTest()
        );

        result.setTotalQuestions(
                total
        );

        result.setCorrectAnswers(
                correct
        );

        result.setPercentage(
                percentage
        );

        result.setPassed(
                passed
        );

        result.setAttemptedAt(
                submittedAt
        );

        result =
                mockTestResultRepository.save(
                        result
                );

        attempt.setMockTestResult(
                result
        );

        attempt.setCompleted(true);

        attempt.setAutoSubmitted(
                automatic ||
                attempt.isAutoSubmitted()
        );

        attempt.setSubmittedAt(
                submittedAt
        );

        testAttemptRepository.save(
                attempt
        );

        /*
         * Existing notification behavior stays
         * functional. AI Interview qualification
         * gate will use this saved PASS result.
         */
        schedulePassEmail(
                attempt.getStudentProfile(),
                result
        );

        return mapToSubmitResponse(
                attempt
        );
    }

    /*
     * =====================================================
     * PASS EMAIL
     * =====================================================
     */
    private void schedulePassEmail(
            StudentProfile student,
            MockTestResult result) {

        if (!result.isPassed() ||
                student.getEmail()
                        == null ||
                student.getEmail()
                        .isBlank()) {

            return;
        }

        String email =
                student.getEmail();

        String name =
                student.getFullName();

        double percentage =
                result.getPercentage();

        Long resultId =
                result.getId();

        /*
         * Email is sent only after
         * DB transaction commits.
         */
        TransactionSynchronizationManager
                .registerSynchronization(
                        new TransactionSynchronization() {

                            @Override
                            public void afterCommit() {

                                try {

                                    emailService
                                            .sendMockTestPassedEmail(
                                                    email,
                                                    name,
                                                    percentage
                                            );

                                } catch (Exception exception) {

                                    log.warn(
                                            "Pass email could not be sent for result {}",
                                            resultId,
                                            exception
                                    );
                                }
                            }
                        }
                );
    }

    /*
     * =====================================================
     * CREATE IMMUTABLE QUESTION SNAPSHOT
     * =====================================================
     */
    private TestAttemptQuestion snapshot(
            TestAttempt attempt,
            Question source,
            int position) {

        String correct =
                source.getCorrectOption();

        if (correct == null ||
                !correct.trim()
                        .toUpperCase(
                                Locale.ROOT
                        )
                        .matches("[ABCD]")) {

            throw error(
                    HttpStatus.CONFLICT,
                    "A question has an invalid answer key."
            );
        }

        TestAttemptQuestion question =
                new TestAttemptQuestion();

        question.setTestAttempt(
                attempt
        );

        question.setQuestionId(
                source.getId()
        );

        question.setQuestionNumber(
                position
        );

        question.setQuestionText(
                source.getQuestionText()
        );

        question.setOptionA(
                source.getOptionA()
        );

        question.setOptionB(
                source.getOptionB()
        );

        question.setOptionC(
                source.getOptionC()
        );

        question.setOptionD(
                source.getOptionD()
        );

        question.setCategory(
                source.getCategory()
        );

        question.setTechnicalSkill(
                source.getTechnicalSkill()
        );

        question.setCorrectOption(
                correct.trim()
                        .toUpperCase(
                                Locale.ROOT
                        )
        );

        question.setSelectedOption(
                null
        );

        return question;
    }

    /*
     * =====================================================
     * RANDOM QUESTION SELECTION - MANUAL ONLY
     * =====================================================
     */
    private List<Question> selectQuestions(
            List<Question> available,
            int count,
            String category) {

        if (available.size()
                < count) {

            throw error(
                    HttpStatus.CONFLICT,
                    "Not enough "
                            + category
                            + " questions available."
            );
        }

        List<Question> shuffled =
                new ArrayList<>(
                        available
                );

        Collections.shuffle(
                shuffled
        );

        return new ArrayList<>(
                shuffled.subList(
                        0,
                        count
                )
        );
    }

    /*
     * =====================================================
     * BASIC MOCK TEST VALIDATION
     * =====================================================
     */
    private void validateTestConfiguration(
            int aptitude,
            int reasoning,
            int technical,
            int duration) {

        long total =
                (long) aptitude
                        + reasoning
                        + technical;

        if (aptitude < 0 ||
                reasoning < 0 ||
                technical < 0 ||
                total <= 0 ||
                total > Integer.MAX_VALUE ||
                duration <= 0) {

            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Question counts must be non-negative, with a positive total and duration."
            );
        }
    }

    private void requireId(
            Long id,
            String label) {

        if (id == null ||
                id <= 0) {

            throw error(
                    HttpStatus.BAD_REQUEST,
                    label
                            + " must be a positive number."
            );
        }
    }

    private ResponseStatusException error(
            HttpStatus status,
            String message) {

        return new ResponseStatusException(
                status,
                message
        );
    }

    /*
     * =====================================================
     * DETECT MANUAL VS PERSONALIZED
     * =====================================================
     */
    private boolean isPersonalizedMockTest(
            Long mockTestId) {

        return mockTestQuestionRepository
                .existsByMockTestId(
                        mockTestId
                );
    }

    /*
     * =====================================================
     * START RESPONSE
     * =====================================================
     */
    private StartMockTestResponse
            mapToStartResponse(
                    TestAttempt attempt,
                    List<TestAttemptQuestion> questions) {

        StartMockTestResponse response =
                new StartMockTestResponse(
                        attempt.getMockTest()
                                .getId(),
                        attempt.getTestTitle(),
                        attempt.getDurationMinutes(),
                        attempt.getPassPercentage(),
                        attempt.getSelectedSkill(),
                        questions.stream()
                                .map(
                                        this::mapToStudentQuestion
                                )
                                .toList()
                );

        response.setTestAttemptId(
                attempt.getId()
        );

        ZoneId serverZone =
                ZoneId.systemDefault();

        response.setStartedAt(
                attempt.getStartedAt()
                        .atZone(serverZone)
                        .toInstant()
        );

        response.setExpiresAt(
                attempt.getExpiresAt()
                        .atZone(serverZone)
                        .toInstant()
        );

        response.setServerTime(
                Instant.now()
        );

        return response;
    }

    /*
     * =====================================================
     * STUDENT-SAFE QUESTION RESPONSE
     * =====================================================
     *
     * Correct answer is NOT returned.
     */
    private StudentQuestionResponse
            mapToStudentQuestion(
                    TestAttemptQuestion question) {

        return new StudentQuestionResponse(
                question.getQuestionId(),
                question.getQuestionText(),
                question.getOptionA(),
                question.getOptionB(),
                question.getOptionC(),
                question.getOptionD(),
                question.getCategory(),
                question.getTechnicalSkill(),
                question.getSelectedOption()
        );
    }

    /*
     * =====================================================
     * RESULT RESPONSE
     * =====================================================
     */
    private SubmitMockTestResponse
            mapToSubmitResponse(
                    TestAttempt attempt) {

        MockTestResult result =
                attempt.getMockTestResult();

        String message =
                result.isPassed()
                        ? "Congratulations! You passed the mock test."
                        : "You did not pass the mock test.";

        if (attempt.isAutoSubmitted()) {

            message =
                    "Test automatically submitted. "
                            + message;
        }

        return new SubmitMockTestResponse(
                attempt.getMockTest().getId(),
                result.getTotalQuestions(),
                result.getCorrectAnswers(),
                result.getPercentage(),
                attempt.getPassPercentage(),
                result.isPassed(),
                message,
                attempt.getId(),
                result.getId(),
                attempt.isAutoSubmitted()
        );
    }

    /*
     * =====================================================
     * GENERAL MOCK TEST RESPONSE
     * =====================================================
     */
    private MockTestResponse mapToResponse(
            MockTest mockTest) {

        MockTestResponse response =
                new MockTestResponse(
                        mockTest.getId(),
                        mockTest.getTitle(),
                        mockTest.getAptitudeQuestionCount(),
                        mockTest.getReasoningQuestionCount(),
                        mockTest.getTechnicalQuestionCount(),
                        mockTest.getDurationMinutes(),
                        mockTest.getPassPercentage(),
                        mockTest.isActive()
                );

        /*
         * Detect whether Faculty is viewing
         * an AI Personalized Mock Test.
         */
        if (isPersonalizedMockTest(
                mockTest.getId())) {

            response.setPersonalized(true);

            List<MockTestQuestion> mappings =
                    mockTestQuestionRepository
                            .findByMockTestIdOrderByQuestionOrderAsc(
                                    mockTest.getId()
                            );

            response.setSelectedSkills(
                    extractTechnicalSkills(
                            mappings
                    )
            );

        } else {

            response.setPersonalized(false);
            response.setSelectedSkills(null);
        }

        return response;
    }

    /*
     * =====================================================
     * STUDENT PERSONALIZED RESPONSE
     * =====================================================
     */
    private MockTestResponse
            mapToAssignedPersonalizedResponse(
                    StudentMockTestAssignment assignment) {

        MockTest mockTest =
                assignment.getMockTest();

        MockTestResponse response =
                mapToResponse(
                        mockTest
                );

        response.setPersonalized(true);

        response.setAssignmentId(
                assignment.getId()
        );

        response.setAssignedAt(
                assignment.getAssignedAt()
        );

        return response;
    }

    /*
     * =====================================================
     * EXTRACT TECHNICAL SKILLS
     * =====================================================
     */
    private List<String> extractTechnicalSkills(
            List<MockTestQuestion> mappings) {

        LinkedHashSet<String> skills =
                new LinkedHashSet<>();

        for (MockTestQuestion mapping
                : mappings) {

            Question question =
                    mapping.getQuestion();

            if (question == null ||
                    question.getCategory()
                            != QuestionCategory.TECHNICAL) {

                continue;
            }

            String skill =
                    question.getTechnicalSkill();

            if (skill != null &&
                    !skill.isBlank()) {

                skills.add(
                        skill.trim()
                );
            }
        }

        return new ArrayList<>(
                skills
        );
    }
}