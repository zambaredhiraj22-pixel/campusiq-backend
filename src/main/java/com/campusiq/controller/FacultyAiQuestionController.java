package com.campusiq.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusiq.dto.AiGeneratedQuestionResponse;
import com.campusiq.dto.AiQuestionBulkApproveRequest;
import com.campusiq.dto.AiQuestionGenerateRequest;
import com.campusiq.dto.QuestionResponse;
import com.campusiq.service.AiQuestionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/faculty/ai/questions")
@RequiredArgsConstructor
public class FacultyAiQuestionController {

    private final AiQuestionService aiQuestionService;

    /*
     * STEP 1:
     * Generate 60 AI draft questions.
     *
     * 15 Aptitude
     * 15 Reasoning
     * 30 Technical
     *
     * Technical questions are distributed across
     * the student's selected faculty-verified skills.
     *
     * Questions are NOT saved to the Question Bank here.
     */
    @PostMapping("/generate")
    public ResponseEntity<
            List<AiGeneratedQuestionResponse>>
            generateQuestions(
                    @Valid
                    @RequestBody
                    AiQuestionGenerateRequest request) {

        List<AiGeneratedQuestionResponse>
                generatedQuestions =
                aiQuestionService
                        .generateQuestions(request);

        return ResponseEntity.ok(
                generatedQuestions
        );
    }

    /*
     * STEP 2:
     * Faculty reviews the generated questions.
     *
     * Faculty may edit/remove/regenerate questions
     * in the frontend.
     *
     * Only the final approved 60-question batch
     * reaches this endpoint.
     *
     * Backend again validates:
     * - exactly 60 questions
     * - 15 Aptitude
     * - 15 Reasoning
     * - 30 Technical
     * - verified technical skills
     * - balanced skill allocation
     * - duplicate questions
     * - valid answer options
     *
     * Only then are questions stored
     * in the official Question Bank.
     */
    @PostMapping("/approve")
    public ResponseEntity<
            List<QuestionResponse>>
            approveQuestions(
                    @Valid
                    @RequestBody
                    AiQuestionBulkApproveRequest request) {

        List<QuestionResponse>
                approvedQuestions =
                aiQuestionService
                        .approveQuestions(request);

        return ResponseEntity.ok(
                approvedQuestions
        );
    }
}