package com.campusiq.service.impl;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.campusiq.dto.ProctoringViolationRequest;
import com.campusiq.dto.ProctoringViolationResponse;
import com.campusiq.dto.SubmitMockTestResponse;
import com.campusiq.dto.TestAttemptRequest;
import com.campusiq.dto.TestAttemptResponse;
import com.campusiq.entity.ProctoringViolation;
import com.campusiq.entity.StudentProfile;
import com.campusiq.entity.TestAttempt;
import com.campusiq.entity.User;
import com.campusiq.enums.ProctoringViolationType;
import com.campusiq.enums.Role;
import com.campusiq.repository.ProctoringViolationRepository;
import com.campusiq.repository.StudentProfileRepository;
import com.campusiq.repository.TestAttemptRepository;
import com.campusiq.repository.UserRepository;
import com.campusiq.service.MockTestService;
import com.campusiq.service.ProctoringService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(isolation = Isolation.READ_COMMITTED)
public class ProctoringServiceImpl implements ProctoringService {

    private static final int MAX_WARNINGS = 2;

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final ProctoringViolationRepository proctoringViolationRepository;
    private final MockTestService mockTestService;

    @Override
    public TestAttemptResponse startTest(
            TestAttemptRequest request,
            String username) {

        getStudent(username);

        if (request == null
                || request.getMockTestId() == null
                || request.getMockTestId() <= 0) {

            throw error(
                    HttpStatus.BAD_REQUEST,
                    "A valid mock test ID is required."
            );
        }

        throw error(
                HttpStatus.CONFLICT,
                "Start the test using POST /api/student/mock-tests/start with mockTestId "
                        + "and selectedSkill. Use its returned testAttemptId for proctoring."
        );
    }

    @Override
    public ProctoringViolationResponse recordViolation(
            ProctoringViolationRequest request,
            String username) {

        if (request == null
                || request.getViolationType() == null) {

            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Violation type is required."
            );
        }

        String description =
                request.getDescription();

        if (description != null) {

            description =
                    description.trim();

            if (description.length() > 500) {

                throw error(
                        HttpStatus.BAD_REQUEST,
                        "Violation description must not exceed 500 characters."
                );
            }
        }

        TestAttempt attempt =
                getOwnedAttempt(
                        request.getTestAttemptId(),
                        username,
                        true
                );

        if (attempt.isCompleted()
                || attempt.getMockTestResult() != null
                || attempt.isAutoSubmitted()
                || attempt.getWarningCount() > MAX_WARNINGS) {

            throw error(
                    HttpStatus.CONFLICT,
                    "This attempt is already closed. Additional violations are not accepted."
            );
        }

        if (attempt.getExpiresAt() == null
                || attempt.getTotalQuestions() == null
                || attempt.getTotalQuestions() <= 0
                || attempt.getSelectedSkill() == null) {

            throw error(
                    HttpStatus.CONFLICT,
                    "This older attempt has no saved test questions. Start a new mock-test attempt."
            );
        }

        LocalDateTime detectedAt =
                LocalDateTime.now();

        if (!detectedAt.isBefore(
                attempt.getExpiresAt())) {

            throw error(
                    HttpStatus.CONFLICT,
                    "Test time has expired. Fetch the attempt result; no new violation was recorded."
            );
        }

        ProctoringViolationType violationType =
                request.getViolationType();

        boolean multipleFaces =
                violationType
                        == ProctoringViolationType.MULTIPLE_FACES;

        long previousPhoneDetections =
                0;

        if (violationType
                == ProctoringViolationType.PHONE_DETECTED) {

            previousPhoneDetections =
                    proctoringViolationRepository
                            .countByTestAttemptIdAndViolationType(
                                    attempt.getId(),
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
                    attempt.getWarningCount() + 1;
        }

        attempt.setWarningCount(
                warningNumber
        );

        ProctoringViolation violation =
                new ProctoringViolation();

        violation.setTestAttempt(
                attempt
        );

        violation.setViolationType(
                violationType
        );

        violation.setDescription(
                description
        );

        violation.setDetectedAt(
                detectedAt
        );

        violation.setWarningNumber(
                warningNumber
        );

        ProctoringViolation savedViolation =
                proctoringViolationRepository.save(
                        violation
                );

        testAttemptRepository.saveAndFlush(
                attempt
        );

        boolean autoSubmitted =
                false;

        String message;

        if (multipleFaces) {

            SubmitMockTestResponse result =
                    mockTestService.getResult(
                            username,
                            attempt.getId()
                    );

            autoSubmitted =
                    result.isAutoSubmitted();

            message =
                    "Multiple faces detected. Test automatically submitted.";

        } else if (secondPhoneDetection) {

            SubmitMockTestResponse result =
                    mockTestService.getResult(
                            username,
                            attempt.getId()
                    );

            autoSubmitted =
                    result.isAutoSubmitted();

            message =
                    "Mobile phone detected for the second time. Test automatically submitted.";

        } else if (warningNumber > MAX_WARNINGS) {

            SubmitMockTestResponse result =
                    mockTestService.getResult(
                            username,
                            attempt.getId()
                    );

            autoSubmitted =
                    result.isAutoSubmitted();

            message =
                    "Test automatically submitted after the third recorded proctoring violation.";

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
                            + ": Proctoring violation recorded.";
        }

        return new ProctoringViolationResponse(
                savedViolation.getId(),
                attempt.getId(),
                savedViolation.getViolationType(),
                savedViolation.getDetectedAt(),
                warningNumber,
                autoSubmitted,
                message
        );
    }

    @Override
    @Transactional(
            readOnly = true,
            isolation = Isolation.READ_COMMITTED
    )
    public TestAttemptResponse getTestAttemptStatus(
            Long testAttemptId,
            String username) {

        TestAttempt attempt =
                getOwnedAttempt(
                        testAttemptId,
                        username,
                        false
                );

        String message =
                "Test attempt status fetched successfully.";

        if (attempt.isCompleted()) {

            message =
                    "This attempt is completed.";

        } else if (
                attempt.getExpiresAt() != null
                        && !LocalDateTime.now()
                        .isBefore(
                                attempt.getExpiresAt()
                        )
        ) {

            message =
                    "Test time has expired. Fetch the attempt result to finalize it.";
        }

        return new TestAttemptResponse(
                attempt.getId(),
                attempt.getMockTest().getId(),
                attempt.getStartedAt(),
                attempt.getWarningCount(),
                attempt.isCompleted(),
                attempt.isAutoSubmitted(),
                message
        );
    }

    private TestAttempt getOwnedAttempt(
            Long testAttemptId,
            String username,
            boolean lock) {

        if (testAttemptId == null
                || testAttemptId <= 0) {

            throw error(
                    HttpStatus.BAD_REQUEST,
                    "A valid test attempt ID is required."
            );
        }

        StudentProfile student =
                getStudent(username);

        TestAttempt attempt =
                (
                        lock
                                ? testAttemptRepository
                                .findByIdForUpdate(
                                        testAttemptId
                                )
                                : testAttemptRepository
                                .findById(
                                        testAttemptId
                                )
                )
                        .orElseThrow(
                                () -> error(
                                        HttpStatus.NOT_FOUND,
                                        "Test attempt not found."
                                )
                        );

        if (!Objects.equals(
                attempt
                        .getStudentProfile()
                        .getId(),
                student.getId())) {

            throw error(
                    HttpStatus.FORBIDDEN,
                    "You cannot access another student's attempt."
            );
        }

        return attempt;
    }

    private StudentProfile getStudent(
            String username) {

        if (username == null
                || username.isBlank()) {

            throw error(
                    HttpStatus.UNAUTHORIZED,
                    "Login is required."
            );
        }

        User user =
                userRepository
                        .findByUsername(
                                username
                        )
                        .orElseThrow(
                                () -> error(
                                        HttpStatus.UNAUTHORIZED,
                                        "User not found."
                                )
                        );

        if (user.getRole()
                != Role.STUDENT) {

            throw error(
                    HttpStatus.FORBIDDEN,
                    "Only students can access test proctoring."
            );
        }

        return studentProfileRepository
                .findByUser(user)
                .orElseThrow(
                        () -> error(
                                HttpStatus.NOT_FOUND,
                                "Student profile not found."
                        )
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