package com.campusiq.service.impl;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.campusiq.dto.AiInterviewAnswerRequest;
import com.campusiq.dto.AiInterviewAnswerResponse;
import com.campusiq.dto.AiInterviewEligibilityResponse;
import com.campusiq.dto.AiInterviewPrepareResponse;
import com.campusiq.dto.AiInterviewQuestionResponse;
import com.campusiq.dto.AiInterviewResultResponse;
import com.campusiq.dto.AiInterviewStartResponse;
import com.campusiq.dto.AiInterviewViolationRequest;
import com.campusiq.dto.AiInterviewViolationResponse;
import com.campusiq.dto.StudentInterviewProfileRequest;
import com.campusiq.dto.StudentInterviewProfileResponse;
import com.campusiq.entity.AiInterviewAnswer;
import com.campusiq.entity.AiInterviewQuestion;
import com.campusiq.entity.AiInterviewSession;
import com.campusiq.entity.AiInterviewViolation;
import com.campusiq.entity.MockTestResult;
import com.campusiq.entity.Skill;
import com.campusiq.entity.StudentInterviewProfile;
import com.campusiq.entity.StudentProfile;
import com.campusiq.entity.User;
import com.campusiq.enums.AiInterviewQuestionType;
import com.campusiq.enums.AiInterviewStatus;
import com.campusiq.enums.ProctoringViolationType;
import com.campusiq.enums.SkillStatus;
import com.campusiq.repository.AiInterviewAnswerRepository;
import com.campusiq.repository.AiInterviewQuestionRepository;
import com.campusiq.repository.AiInterviewSessionRepository;
import com.campusiq.repository.AiInterviewViolationRepository;
import com.campusiq.repository.MockTestResultRepository;
import com.campusiq.repository.SkillRepository;
import com.campusiq.repository.StudentInterviewProfileRepository;
import com.campusiq.repository.StudentProfileRepository;
import com.campusiq.repository.UserRepository;
import com.campusiq.service.AiInterviewProvider;
import com.campusiq.service.AiInterviewProvider.AnswerEvaluation;
import com.campusiq.service.AiInterviewProvider.GeneratedInterviewQuestion;
import com.campusiq.service.AiInterviewService;
import com.campusiq.service.EmailService;
import com.campusiq.service.ResumePdfService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiInterviewServiceImpl
        implements AiInterviewService {

    private static final int POOL_SIZE = 25;
    private static final int INTERVIEW_QUESTION_COUNT = 10;

    private static final int TECHNICAL_SELECTED = 5;
    private static final int RESUME_SELECTED = 3;
    private static final int BEHAVIORAL_SELECTED = 2;

    private static final int DEFAULT_DURATION_MINUTES = 30;
    private static final double PASS_PERCENTAGE = 25.0;

    private static final int MAX_WARNINGS = 2;

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final SkillRepository skillRepository;
    private final MockTestResultRepository mockTestResultRepository;

    private final StudentInterviewProfileRepository interviewProfileRepository;
    private final AiInterviewSessionRepository interviewSessionRepository;
    private final AiInterviewQuestionRepository interviewQuestionRepository;
    private final AiInterviewAnswerRepository interviewAnswerRepository;
    private final AiInterviewViolationRepository interviewViolationRepository;

    private final AiInterviewProvider aiInterviewProvider;
    private final EmailService emailService;
    private final ResumePdfService resumePdfService;

    private final SecureRandom secureRandom =
            new SecureRandom();

    @Override
    @Transactional
    public StudentInterviewProfileResponse saveOrUpdateProfile(
            String username,
            StudentInterviewProfileRequest request) {

        StudentProfile student =
                getStudentProfile(username);

        StudentInterviewProfile profile =
                interviewProfileRepository
                        .findByStudentProfile(student)
                        .orElseGet(
                                StudentInterviewProfile::new
                        );

        profile.setStudentProfile(student);
        profile.setResumeFileName(request.getResumeFileName());
        profile.setResumeContentType(request.getResumeContentType());
        profile.setResumeText(request.getResumeText().trim());
        profile.setProjects(trimNullable(request.getProjects()));
        profile.setTechnologies(
                trimNullable(request.getTechnologies())
        );

        profile =
                interviewProfileRepository.save(profile);

        return toProfileResponse(profile);
    }

    @Override
    @Transactional
    public StudentInterviewProfileResponse uploadResume(
            String username,
            MultipartFile file) {

        StudentProfile student =
                getStudentProfile(username);

        String extractedText =
                resumePdfService.extractText(file);

        StudentInterviewProfile profile =
                interviewProfileRepository
                        .findByStudentProfile(student)
                        .orElseGet(
                                StudentInterviewProfile::new
                        );

        profile.setStudentProfile(student);

        String fileName =
                file.getOriginalFilename();

        if (fileName == null ||
                fileName.isBlank()) {

            fileName = "resume.pdf";
        }

        fileName =
                fileName.replace("\\", "/");

        int lastSlash =
                fileName.lastIndexOf('/');

        if (lastSlash >= 0) {

            fileName =
                    fileName.substring(
                            lastSlash + 1
                    );
        }

        profile.setResumeFileName(fileName);
        profile.setResumeContentType("application/pdf");
        profile.setResumeText(extractedText);

        profile =
                interviewProfileRepository.save(profile);

        return toProfileResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentInterviewProfileResponse getProfile(
            String username) {

        StudentProfile student =
                getStudentProfile(username);

        StudentInterviewProfile profile =
                interviewProfileRepository
                        .findByStudentProfile(student)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Interview profile not found"
                                )
                        );

        return toProfileResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public AiInterviewEligibilityResponse checkEligibility(
            String username) {

        StudentProfile student =
                getStudentProfile(username);

        List<MockTestResult> passedResults =
                mockTestResultRepository
                        .findPassedPersonalizedResults(
                                student.getId()
                        );

        boolean profileAvailable =
                interviewProfileRepository
                        .existsByStudentProfileId(
                                student.getId()
                        );

        if (passedResults.isEmpty()) {

            return new AiInterviewEligibilityResponse(
                    student.getId(),
                    false,
                    "Pass a personalized AI mock test to unlock the AI interview",
                    false,
                    profileAvailable,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        MockTestResult result =
                passedResults.get(0);

        AiInterviewSession session =
                interviewSessionRepository
                        .findByMockTestResultId(
                                result.getId()
                        )
                        .orElse(null);

        boolean eligible =
                profileAvailable;

        String message =
                profileAvailable
                        ? "Student is eligible for AI interview"
                        : "Create the interview profile before preparing the AI interview";

        return new AiInterviewEligibilityResponse(
                student.getId(),
                eligible,
                message,
                true,
                profileAvailable,
                result.getId(),
                result.getMockTest().getId(),
                result.getMockTest().getTitle(),
                result.getPercentage(),
                session == null
                        ? null
                        : session.getId(),
                session == null
                        ? null
                        : session.getStatus()
        );
    }

    @Override
    @Transactional
    public AiInterviewPrepareResponse prepareInterview(
            String username,
            Long mockTestResultId) {

        StudentProfile student =
                getStudentProfile(username);

        MockTestResult result =
                getQualifyingResult(
                        student,
                        mockTestResultId
                );

        StudentInterviewProfile profile =
                interviewProfileRepository
                        .findByStudentProfile(student)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "Create interview profile before preparing interview"
                                )
                        );

        List<String> verifiedSkills =
                getVerifiedSkills(student);

        if (verifiedSkills.isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Student does not have any verified technical skills"
            );
        }

        AiInterviewSession existing =
                interviewSessionRepository
                        .findByMockTestResultId(
                                result.getId()
                        )
                        .orElse(null);

        if (existing != null) {

            return buildPrepareResponse(
                    existing,
                    verifiedSkills,
                    "AI interview is already prepared"
            );
        }

        List<GeneratedInterviewQuestion> generated =
                aiInterviewProvider.generateQuestions(
                        profile.getResumeText(),
                        profile.getProjects(),
                        profile.getTechnologies(),
                        verifiedSkills
                );

        if (generated.size() != POOL_SIZE) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI provider must generate exactly 25 interview questions"
            );
        }

        AiInterviewSession session =
                new AiInterviewSession();

        session.setStudentProfile(student);
        session.setInterviewProfile(profile);
        session.setMockTestResult(result);
        session.setStatus(AiInterviewStatus.READY);
        session.setQuestionCount(INTERVIEW_QUESTION_COUNT);
        session.setDurationMinutes(DEFAULT_DURATION_MINUTES);
        session.setPassPercentage(PASS_PERCENTAGE);
        session.setWarningCount(0);
        session.setAutoSubmitted(false);

        session =
                interviewSessionRepository.save(session);

        int poolOrder = 1;

        List<AiInterviewQuestion> questions =
                new ArrayList<>();

        for (GeneratedInterviewQuestion generatedQuestion
                : generated) {

            AiInterviewQuestion question =
                    new AiInterviewQuestion();

            question.setInterviewSession(session);
            question.setQuestionType(
                    generatedQuestion.questionType()
            );
            question.setQuestionText(
                    generatedQuestion.questionText().trim()
            );
            question.setTechnicalSkill(
                    trimNullable(
                            generatedQuestion.technicalSkill()
                    )
            );
            question.setReferenceAnswer(
                    generatedQuestion
                            .referenceAnswer()
                            .trim()
            );
            question.setEvaluationCriteria(
                    generatedQuestion
                            .evaluationCriteria()
                            .trim()
            );
            question.setPoolOrder(poolOrder++);
            question.setSelectedForInterview(false);
            question.setInterviewOrder(null);

            questions.add(question);
        }

        interviewQuestionRepository.saveAll(questions);

        selectInterviewQuestions(session);

        return buildPrepareResponse(
                session,
                verifiedSkills,
                "AI interview prepared successfully"
        );
    }

    @Override
    @Transactional
    public AiInterviewStartResponse startInterview(
            String username,
            Long interviewSessionId) {

        StudentProfile student =
                getStudentProfile(username);

        AiInterviewSession session =
                getOwnedSession(
                        student,
                        interviewSessionId
                );

        if (session.getStatus()
                == AiInterviewStatus.READY) {

            Instant now =
                    Instant.now();

            session.setStatus(
                    AiInterviewStatus.IN_PROGRESS
            );

            session.setStartedAt(now);

            session.setExpiresAt(
                    now.plus(
                            session.getDurationMinutes(),
                            ChronoUnit.MINUTES
                    )
            );

            interviewSessionRepository.save(session);

        } else if (session.getStatus()
                != AiInterviewStatus.IN_PROGRESS) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Interview cannot be started in its current state"
            );
        }

        ensureNotExpired(session);

        return buildStartResponse(session);
    }

    @Override
    @Transactional(readOnly = true)
    public AiInterviewStartResponse getInterview(
            String username,
            Long interviewSessionId) {

        StudentProfile student =
                getStudentProfile(username);

        AiInterviewSession session =
                getOwnedSession(
                        student,
                        interviewSessionId
                );

        if (session.getStatus()
                != AiInterviewStatus.IN_PROGRESS) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Interview is not currently in progress"
            );
        }

        ensureNotExpired(session);

        return buildStartResponse(session);
    }

    @Override
    @Transactional
    public AiInterviewAnswerResponse saveAnswer(
            String username,
            Long interviewSessionId,
            AiInterviewAnswerRequest request) {

        StudentProfile student =
                getStudentProfile(username);

        AiInterviewSession session =
                getOwnedSession(
                        student,
                        interviewSessionId
                );

        ensureInterviewInProgress(session);
        ensureNotExpired(session);

        AiInterviewQuestion question =
                interviewQuestionRepository
                        .findById(request.getQuestionId())
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Interview question not found"
                                )
                        );

        if (!question.getInterviewSession()
                .getId()
                .equals(session.getId())
                || !Boolean.TRUE.equals(
                        question.getSelectedForInterview()
                )) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Question does not belong to this interview"
            );
        }

        AiInterviewAnswer answer =
                interviewAnswerRepository
                        .findByInterviewSessionIdAndInterviewQuestionId(
                                session.getId(),
                                question.getId()
                        )
                        .orElseGet(
                                AiInterviewAnswer::new
                        );

        answer.setInterviewSession(session);
        answer.setInterviewQuestion(question);

        answer.setAnswerText(
                request.getAnswerText().trim()
        );

        answer.setEvaluated(false);
        answer.setContentScore(null);
        answer.setRelevanceScore(null);
        answer.setCommunicationScore(null);
        answer.setOverallScore(null);
        answer.setAiFeedback(null);
        answer.setEvaluatedAt(null);

        answer =
                interviewAnswerRepository.save(answer);

        return new AiInterviewAnswerResponse(
                answer.getId(),
                session.getId(),
                question.getId(),
                answer.getAnswerText(),
                answer.getEvaluated(),
                answer.getAnsweredAt(),
                answer.getUpdatedAt()
        );
    }

    @Override
    @Transactional
    public AiInterviewViolationResponse reportViolation(
            String username,
            Long interviewSessionId,
            AiInterviewViolationRequest request) {

        StudentProfile student =
                getStudentProfile(username);

        AiInterviewSession session =
                getOwnedSession(
                        student,
                        interviewSessionId
                );

        ensureInterviewInProgress(session);
        ensureNotExpired(session);

        if (request == null
                || request.getViolationType() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Violation type is required"
            );
        }

        ProctoringViolationType violationType =
                request.getViolationType();

        boolean multipleFaces =
                violationType
                        == ProctoringViolationType.MULTIPLE_FACES;

        long previousPhoneDetections = 0;

        if (violationType
                == ProctoringViolationType.PHONE_DETECTED) {

            previousPhoneDetections =
                    interviewViolationRepository
                            .countByInterviewSessionIdAndViolationType(
                                    session.getId(),
                                    ProctoringViolationType.PHONE_DETECTED
                            );
        }

        boolean secondPhoneDetection =
                violationType
                        == ProctoringViolationType.PHONE_DETECTED
                        && previousPhoneDetections >= 1;

        boolean immediateAutoSubmit =
                multipleFaces
                        || secondPhoneDetection;

        int warningNumber;

        if (immediateAutoSubmit) {

            warningNumber =
                    MAX_WARNINGS + 1;

        } else {

            warningNumber =
                    session.getWarningCount() + 1;
        }

        session.setWarningCount(
                warningNumber
        );

        AiInterviewViolation violation =
                new AiInterviewViolation();

        violation.setInterviewSession(
                session
        );

        violation.setViolationType(
                violationType
        );

        violation.setWarningNumber(
                warningNumber
        );

        violation.setDescription(
                trimNullable(
                        request.getDescription()
                )
        );

        violation =
                interviewViolationRepository.save(
                        violation
                );

        boolean autoSubmitted =
                immediateAutoSubmit
                        || warningNumber > MAX_WARNINGS;

        if (autoSubmitted) {

            finalizeSession(
                    session,
                    true
            );

        } else {

            interviewSessionRepository.save(
                    session
            );
        }

        String message;

        if (multipleFaces) {

            message =
                    "Multiple faces detected. Interview automatically submitted.";

        } else if (secondPhoneDetection) {

            message =
                    "Mobile phone detected for the second time. Interview automatically submitted.";

        } else if (autoSubmitted) {

            message =
                    "Third proctoring violation detected. Interview automatically submitted.";

        } else if (violationType
                == ProctoringViolationType.PHONE_DETECTED) {

            message =
                    "Mobile phone detected. Warning recorded.";

        } else if (violationType
                == ProctoringViolationType.SECOND_PERSON_DETECTED) {

            message =
                    "Another person was detected near the student. Warning recorded.";

        } else {

            message =
                    "Warning "
                            + warningNumber
                            + " recorded";
        }

        return new AiInterviewViolationResponse(
                violation.getId(),
                session.getId(),
                violation.getViolationType(),
                warningNumber,
                violation.getDescription(),
                violation.getDetectedAt(),
                autoSubmitted,
                session.getStatus(),
                message
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiInterviewViolationResponse>
            getViolationHistory(
                    String username,
                    Long interviewSessionId) {

        StudentProfile student =
                getStudentProfile(username);

        AiInterviewSession session =
                getOwnedSession(
                        student,
                        interviewSessionId
                );

        return interviewViolationRepository
                .findByInterviewSessionIdOrderByDetectedAtAsc(
                        session.getId()
                )
                .stream()
                .map(violation ->
                        new AiInterviewViolationResponse(
                                violation.getId(),
                                session.getId(),
                                violation.getViolationType(),
                                violation.getWarningNumber(),
                                violation.getDescription(),
                                violation.getDetectedAt(),
                                Boolean.TRUE.equals(
                                        session.getAutoSubmitted()
                                ),
                                session.getStatus(),
                                "Violation recorded"
                        )
                )
                .toList();
    }

    @Override
    @Transactional
    public AiInterviewResultResponse submitInterview(
            String username,
            Long interviewSessionId) {

        StudentProfile student =
                getStudentProfile(username);

        AiInterviewSession session =
                getOwnedSession(
                        student,
                        interviewSessionId
                );

        if (session.getStatus()
                == AiInterviewStatus.COMPLETED
                || session.getStatus()
                        == AiInterviewStatus.AUTO_SUBMITTED) {

            return buildResultResponse(session);
        }

        ensureInterviewInProgress(session);

        boolean expired =
                session.getExpiresAt() != null
                        && !Instant.now()
                        .isBefore(
                                session.getExpiresAt()
                        );

        finalizeSession(
                session,
                expired
        );

        return buildResultResponse(session);
    }

    @Override
    @Transactional
    public AiInterviewResultResponse getResult(
            String username,
            Long interviewSessionId) {

        StudentProfile student =
                getStudentProfile(username);

        AiInterviewSession session =
                getOwnedSession(
                        student,
                        interviewSessionId
                );

        if (session.getStatus()
                == AiInterviewStatus.IN_PROGRESS
                && session.getExpiresAt() != null
                && !Instant.now()
                        .isBefore(
                                session.getExpiresAt()
                        )) {

            finalizeSession(
                    session,
                    true
            );
        }

        if (session.getStatus()
                != AiInterviewStatus.COMPLETED
                && session.getStatus()
                        != AiInterviewStatus.AUTO_SUBMITTED) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Interview result is not available yet"
            );
        }

        return buildResultResponse(session);
    }

    @Override
    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void finalizeExpiredInterviews() {

        List<AiInterviewSession> sessions =
                interviewSessionRepository
                        .findByStatus(
                                AiInterviewStatus.IN_PROGRESS
                        );

        Instant now =
                Instant.now();

        for (AiInterviewSession session
                : sessions) {

            if (session.getExpiresAt() == null
                    || now.isBefore(
                            session.getExpiresAt()
                    )) {

                continue;
            }

            try {

                finalizeSession(
                        session,
                        true
                );

            } catch (RuntimeException ignored) {
            }
        }
    }

    private void selectInterviewQuestions(
            AiInterviewSession session) {

        List<AiInterviewQuestion> technical =
                new ArrayList<>(
                        interviewQuestionRepository
                                .findByInterviewSessionIdAndQuestionTypeOrderByPoolOrderAsc(
                                        session.getId(),
                                        AiInterviewQuestionType.TECHNICAL
                                )
                );

        List<AiInterviewQuestion> resume =
                new ArrayList<>(
                        interviewQuestionRepository
                                .findByInterviewSessionIdAndQuestionTypeOrderByPoolOrderAsc(
                                        session.getId(),
                                        AiInterviewQuestionType.RESUME_PROJECT
                                )
                );

        List<AiInterviewQuestion> behavioral =
                new ArrayList<>(
                        interviewQuestionRepository
                                .findByInterviewSessionIdAndQuestionTypeOrderByPoolOrderAsc(
                                        session.getId(),
                                        AiInterviewQuestionType.BEHAVIORAL
                                )
                );

        if (technical.size() < TECHNICAL_SELECTED
                || resume.size() < RESUME_SELECTED
                || behavioral.size() < BEHAVIORAL_SELECTED) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI interview question distribution is invalid"
            );
        }

        Collections.shuffle(
                technical,
                secureRandom
        );

        Collections.shuffle(
                resume,
                secureRandom
        );

        Collections.shuffle(
                behavioral,
                secureRandom
        );

        List<AiInterviewQuestion> selected =
                new ArrayList<>();

        selected.addAll(
                technical.subList(
                        0,
                        TECHNICAL_SELECTED
                )
        );

        selected.addAll(
                resume.subList(
                        0,
                        RESUME_SELECTED
                )
        );

        selected.addAll(
                behavioral.subList(
                        0,
                        BEHAVIORAL_SELECTED
                )
        );

        Collections.shuffle(
                selected,
                secureRandom
        );

        int order = 1;

        for (AiInterviewQuestion question
                : selected) {

            question.setSelectedForInterview(true);
            question.setInterviewOrder(order++);
        }

        interviewQuestionRepository.saveAll(selected);

        long selectedCount =
                interviewQuestionRepository
                        .countByInterviewSessionIdAndSelectedForInterviewTrue(
                                session.getId()
                        );

        if (selectedCount
                != INTERVIEW_QUESTION_COUNT) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to prepare 10 interview questions"
            );
        }
    }

    private void finalizeSession(
            AiInterviewSession session,
            boolean autoSubmitted) {

        List<AiInterviewQuestion> questions =
                interviewQuestionRepository
                        .findByInterviewSessionIdAndSelectedForInterviewTrueOrderByInterviewOrderAsc(
                                session.getId()
                        );

        if (questions.size()
                != INTERVIEW_QUESTION_COUNT) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Interview does not contain exactly 10 selected questions"
            );
        }

        List<AiInterviewAnswer> answers =
                interviewAnswerRepository
                        .findByInterviewSessionIdOrderByInterviewQuestionInterviewOrderAsc(
                                session.getId()
                        );

        Map<Long, AiInterviewAnswer> answerMap =
                answers.stream()
                        .collect(
                                Collectors.toMap(
                                        answer ->
                                                answer.getInterviewQuestion()
                                                        .getId(),
                                        Function.identity()
                                )
                        );

        double technicalContent = 0.0;
        int technicalCount = 0;

        double resumeContent = 0.0;
        int resumeCount = 0;

        double relevanceTotal = 0.0;
        double communicationTotal = 0.0;

        List<String> feedbacks =
                new ArrayList<>();

        for (AiInterviewQuestion question
                : questions) {

            AiInterviewAnswer answer =
                    answerMap.get(
                            question.getId()
                    );

            double content = 0.0;
            double relevance = 0.0;
            double communication = 0.0;

            if (answer != null
                    && answer.getAnswerText() != null
                    && !answer.getAnswerText().isBlank()) {

                if (!Boolean.TRUE.equals(
                        answer.getEvaluated()
                )) {

                    AnswerEvaluation evaluation =
                            aiInterviewProvider
                                    .evaluateAnswer(
                                            question.getQuestionText(),
                                            question.getReferenceAnswer(),
                                            question.getEvaluationCriteria(),
                                            answer.getAnswerText(),
                                            question.getQuestionType()
                                    );

                    answer.setContentScore(
                            evaluation.contentScore()
                    );

                    answer.setRelevanceScore(
                            evaluation.relevanceScore()
                    );

                    answer.setCommunicationScore(
                            evaluation.communicationScore()
                    );

                    answer.setOverallScore(
                            evaluation.overallScore()
                    );

                    answer.setAiFeedback(
                            evaluation.feedback()
                    );

                    answer.setEvaluated(true);

                    answer.setEvaluatedAt(
                            Instant.now()
                    );

                    interviewAnswerRepository.save(answer);
                }

                content =
                        safeScore(
                                answer.getContentScore()
                        );

                relevance =
                        safeScore(
                                answer.getRelevanceScore()
                        );

                communication =
                        safeScore(
                                answer.getCommunicationScore()
                        );

                if (answer.getAiFeedback() != null
                        && !answer.getAiFeedback().isBlank()) {

                    feedbacks.add(
                            answer.getAiFeedback()
                    );
                }

            } else {

                feedbacks.add(
                        "No answer was provided for interview question "
                                + question.getInterviewOrder()
                                + "."
                );
            }

            if (question.getQuestionType()
                    == AiInterviewQuestionType.TECHNICAL) {

                technicalContent += content;
                technicalCount++;

            } else if (question.getQuestionType()
                    == AiInterviewQuestionType.RESUME_PROJECT) {

                resumeContent += content;
                resumeCount++;
            }

            relevanceTotal += relevance;
            communicationTotal += communication;
        }

        double technicalScore =
                technicalCount == 0
                        ? 0.0
                        : (technicalContent
                        / technicalCount)
                        * 0.50;

        double resumeProjectScore =
                resumeCount == 0
                        ? 0.0
                        : (resumeContent
                        / resumeCount)
                        * 0.25;

        double relevanceScore =
                (relevanceTotal
                        / INTERVIEW_QUESTION_COUNT)
                        * 0.15;

        double communicationScore =
                (communicationTotal
                        / INTERVIEW_QUESTION_COUNT)
                        * 0.10;

        technicalScore =
                round(technicalScore);

        resumeProjectScore =
                round(resumeProjectScore);

        relevanceScore =
                round(relevanceScore);

        communicationScore =
                round(communicationScore);

        double totalScore =
                round(
                        technicalScore
                                + resumeProjectScore
                                + relevanceScore
                                + communicationScore
                );

        boolean passed =
                totalScore >= session.getPassPercentage();

        String overallFeedback =
                aiInterviewProvider
                        .generateOverallFeedback(
                                feedbacks
                        );

        session.setTechnicalScore(
                technicalScore
        );

        session.setResumeProjectScore(
                resumeProjectScore
        );

        session.setRelevanceScore(
                relevanceScore
        );

        session.setCommunicationScore(
                communicationScore
        );

        session.setTotalScore(
                totalScore
        );

        session.setPassed(
                passed
        );

        session.setOverallFeedback(
                overallFeedback
        );

        session.setSubmittedAt(
                Instant.now()
        );

        session.setAutoSubmitted(
                autoSubmitted
        );

        session.setStatus(
                autoSubmitted
                        ? AiInterviewStatus.AUTO_SUBMITTED
                        : AiInterviewStatus.COMPLETED
        );

        interviewSessionRepository.save(session);

        if (passed) {

            StudentProfile student =
                    session.getStudentProfile();

            boolean alreadyPlacementReady =
                    Boolean.TRUE.equals(
                            student.getPlacementReady()
                    );

            student.setPlacementReady(true);

            studentProfileRepository.save(student);

            if (!alreadyPlacementReady) {

                String email =
                        student.getEmail();

                String name =
                        student.getFullName();

                double mockTestPercentage =
                        session.getMockTestResult()
                                .getPercentage();

                double interviewScore =
                        totalScore;

                Long sessionId =
                        session.getId();

                TransactionSynchronizationManager
                        .registerSynchronization(
                                new TransactionSynchronization() {

                                    @Override
                                    public void afterCommit() {

                                        try {

                                            emailService
                                                    .sendPlacementReadyEmail(
                                                            email,
                                                            name,
                                                            mockTestPercentage,
                                                            interviewScore
                                                    );

                                        } catch (Exception exception) {

                                            log.warn(
                                                    "Placement-ready email could not be sent for interview session {}",
                                                    sessionId,
                                                    exception
                                            );
                                        }
                                    }
                                }
                        );
            }
        }
    }

    private AiInterviewStartResponse buildStartResponse(
            AiInterviewSession session) {

        List<AiInterviewQuestion> questions =
                interviewQuestionRepository
                        .findByInterviewSessionIdAndSelectedForInterviewTrueOrderByInterviewOrderAsc(
                                session.getId()
                        );

        List<AiInterviewQuestionResponse> responses =
                new ArrayList<>();

        for (AiInterviewQuestion question
                : questions) {

            String savedAnswer =
                    interviewAnswerRepository
                            .findByInterviewSessionIdAndInterviewQuestionId(
                                    session.getId(),
                                    question.getId()
                            )
                            .map(
                                    AiInterviewAnswer::getAnswerText
                            )
                            .orElse(null);

            responses.add(
                    new AiInterviewQuestionResponse(
                            question.getId(),
                            question.getQuestionText(),
                            question.getQuestionType(),
                            question.getTechnicalSkill(),
                            question.getInterviewOrder(),
                            savedAnswer
                    )
            );
        }

        return new AiInterviewStartResponse(
                session.getId(),
                session.getStatus(),
                session.getDurationMinutes(),
                session.getPassPercentage(),
                session.getWarningCount(),
                session.getStartedAt(),
                session.getExpiresAt(),
                Instant.now(),
                responses
        );
    }

    private AiInterviewPrepareResponse buildPrepareResponse(
            AiInterviewSession session,
            List<String> verifiedSkills,
            String message) {

        long poolCount =
                interviewQuestionRepository
                        .countByInterviewSessionId(
                                session.getId()
                        );

        long selectedCount =
                interviewQuestionRepository
                        .countByInterviewSessionIdAndSelectedForInterviewTrue(
                                session.getId()
                        );

        return new AiInterviewPrepareResponse(
                session.getId(),
                session.getStudentProfile().getId(),
                session.getInterviewProfile().getId(),
                session.getMockTestResult().getId(),
                session.getStatus(),
                verifiedSkills,
                (int) poolCount,
                (int) selectedCount,
                session.getDurationMinutes(),
                session.getPassPercentage(),
                session.getCreatedAt(),
                message
        );
    }

    private AiInterviewResultResponse buildResultResponse(
            AiInterviewSession session) {

        int answered =
                (int) interviewAnswerRepository
                        .countByInterviewSessionId(
                                session.getId()
                        );

        String message =
                Boolean.TRUE.equals(
                        session.getPassed()
                )
                        ? "AI interview passed successfully"
                        : "AI interview completed but passing criteria were not met";

        return new AiInterviewResultResponse(
                session.getId(),
                session.getStatus(),
                session.getQuestionCount(),
                answered,
                session.getTechnicalScore(),
                session.getResumeProjectScore(),
                session.getRelevanceScore(),
                session.getCommunicationScore(),
                session.getTotalScore(),
                session.getPassPercentage(),
                session.getPassed(),
                session.getAutoSubmitted(),
                session.getOverallFeedback(),
                session.getSubmittedAt(),
                message
        );
    }

    private MockTestResult getQualifyingResult(
            StudentProfile student,
            Long mockTestResultId) {

        List<MockTestResult> results =
                mockTestResultRepository
                        .findPassedPersonalizedResults(
                                student.getId()
                        );

        return results.stream()
                .filter(result ->
                        result.getId()
                                .equals(mockTestResultId)
                )
                .findFirst()
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "A passed personalized AI mock test is required"
                        )
                );
    }

    private List<String> getVerifiedSkills(
            StudentProfile student) {

        List<Skill> skills =
                skillRepository
                        .findByStudentProfileAndStatus(
                                student,
                                SkillStatus.VERIFIED
                        );

        return skills.stream()
                .map(Skill::getSkillName)
                .filter(skill ->
                        skill != null
                                && !skill.isBlank()
                )
                .map(String::trim)
                .distinct()
                .toList();
    }

    private StudentProfile getStudentProfile(
            String username) {

        User user =
                userRepository
                        .findByUsername(username)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "Authenticated user not found"
                                )
                        );

        return studentProfileRepository
                .findByUser(user)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Student profile not found"
                        )
                );
    }

    private AiInterviewSession getOwnedSession(
            StudentProfile student,
            Long sessionId) {

        AiInterviewSession session =
                interviewSessionRepository
                        .findById(sessionId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "AI interview session not found"
                                )
                        );

        if (!session.getStudentProfile()
                .getId()
                .equals(student.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not own this AI interview session"
            );
        }

        return session;
    }

    private void ensureInterviewInProgress(
            AiInterviewSession session) {

        if (session.getStatus()
                != AiInterviewStatus.IN_PROGRESS) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "AI interview is not in progress"
            );
        }
    }

    private void ensureNotExpired(
            AiInterviewSession session) {

        if (session.getExpiresAt() != null
                && !Instant.now()
                        .isBefore(
                                session.getExpiresAt()
                        )) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "AI interview time has expired"
            );
        }
    }

    private StudentInterviewProfileResponse toProfileResponse(
            StudentInterviewProfile profile) {

        return new StudentInterviewProfileResponse(
                profile.getId(),
                profile.getStudentProfile().getId(),
                profile.getResumeFileName(),
                profile.getResumeContentType(),
                profile.getResumeText(),
                profile.getProjects(),
                profile.getTechnologies(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    private String trimNullable(
            String value) {

        if (value == null) {
            return null;
        }

        String trimmed =
                value.trim();

        return trimmed.isEmpty()
                ? null
                : trimmed;
    }

    private double safeScore(
            Double score) {

        return score == null
                ? 0.0
                : Math.max(
                        0.0,
                        Math.min(
                                100.0,
                                score
                        )
                );
    }

    private double round(
            double value) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }
}