package com.campusiq.service.impl;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.campusiq.dto.QuestionRequest;
import com.campusiq.dto.QuestionResponse;
import com.campusiq.entity.Question;
import com.campusiq.enums.QuestionCategory;
import com.campusiq.repository.QuestionRepository;
import com.campusiq.service.QuestionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;

    @Override
    public QuestionResponse addQuestion(
            QuestionRequest request) {

        validateRequest(request);

        String questionText =
                request.getQuestionText().trim();

        if (questionRepository
                .existsByQuestionTextIgnoreCase(
                        questionText)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Question already exists in the Question Bank"
            );
        }

        Question question = new Question();

        applyQuestionData(
                question,
                request
        );

        Question savedQuestion =
                questionRepository.save(question);

        return mapToResponse(savedQuestion);
    }

    @Override
    public List<QuestionResponse> getAllQuestions() {

        return questionRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public QuestionResponse getQuestionById(
            Long id) {

        Question question =
                findQuestionById(id);

        return mapToResponse(question);
    }

    @Override
    public QuestionResponse updateQuestion(
            Long id,
            QuestionRequest request) {

        validateRequest(request);

        Question question =
                findQuestionById(id);

        String questionText =
                request.getQuestionText().trim();

        boolean duplicateExists =
                questionRepository
                        .existsByQuestionTextIgnoreCaseAndIdNot(
                                questionText,
                                id
                        );

        if (duplicateExists) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Another question with the same text already exists"
            );
        }

        applyQuestionData(
                question,
                request
        );

        Question updatedQuestion =
                questionRepository.save(question);

        return mapToResponse(updatedQuestion);
    }

    @Override
    public void deleteQuestion(Long id) {

        Question question =
                findQuestionById(id);

        questionRepository.delete(question);
    }

    private Question findQuestionById(
            Long id) {

        if (id == null || id <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Question ID must be a positive number"
            );
        }

        return questionRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Question not found"
                        )
                );
    }

    private void applyQuestionData(
            Question question,
            QuestionRequest request) {

        question.setQuestionText(
                request.getQuestionText().trim()
        );

        question.setOptionA(
                request.getOptionA().trim()
        );

        question.setOptionB(
                request.getOptionB().trim()
        );

        question.setOptionC(
                request.getOptionC().trim()
        );

        question.setOptionD(
                request.getOptionD().trim()
        );

        question.setCorrectOption(
                request.getCorrectOption()
                        .trim()
                        .toUpperCase()
        );

        question.setCategory(
                request.getCategory()
        );

        if (request.getCategory()
                == QuestionCategory.TECHNICAL) {

            question.setTechnicalSkill(
                    request.getTechnicalSkill().trim()
            );

        } else {

            question.setTechnicalSkill(null);
        }
    }

    private QuestionResponse mapToResponse(
            Question question) {

        return new QuestionResponse(
                question.getId(),
                question.getQuestionText(),
                question.getOptionA(),
                question.getOptionB(),
                question.getOptionC(),
                question.getOptionD(),
                question.getCorrectOption(),
                question.getCategory(),
                question.getTechnicalSkill()
        );
    }

    private void validateRequest(
            QuestionRequest request) {

        if (request == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Question request is required"
            );
        }

        validateRequiredText(
                request.getQuestionText(),
                "Question text"
        );

        if (request.getQuestionText()
                .trim()
                .length() > 500) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Question text cannot exceed 500 characters"
            );
        }

        validateOption(
                request.getOptionA(),
                "Option A"
        );

        validateOption(
                request.getOptionB(),
                "Option B"
        );

        validateOption(
                request.getOptionC(),
                "Option C"
        );

        validateOption(
                request.getOptionD(),
                "Option D"
        );

        if (request.getCategory() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Question category is required"
            );
        }

        if (request.getCorrectOption() == null ||
                !request.getCorrectOption()
                        .trim()
                        .matches("(?i)[A-D]")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Correct option must be A, B, C or D"
            );
        }

        if (request.getCategory()
                == QuestionCategory.TECHNICAL) {

            validateRequiredText(
                    request.getTechnicalSkill(),
                    "Technical skill"
            );
        }
    }

    private void validateOption(
            String option,
            String fieldName) {

        validateRequiredText(
                option,
                fieldName
        );

        if (option.trim().length() > 255) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    fieldName
                            + " cannot exceed 255 characters"
            );
        }
    }

    private void validateRequiredText(
            String value,
            String fieldName) {

        if (value == null ||
                value.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    fieldName + " is required"
            );
        }
    }
}