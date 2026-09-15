package com.campusiq.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusiq.dto.QuestionRequest;
import com.campusiq.dto.QuestionResponse;
import com.campusiq.service.QuestionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/faculty/questions")
@RequiredArgsConstructor
public class FacultyQuestionController {

    private final QuestionService questionService;

    @PostMapping
    public ResponseEntity<QuestionResponse> addQuestion(
            @RequestBody QuestionRequest request) {

        return ResponseEntity.ok(questionService.addQuestion(request));
    }
    

    @GetMapping
    public ResponseEntity<List<QuestionResponse>> getAllQuestions() {

        return ResponseEntity.ok(questionService.getAllQuestions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuestionResponse> getQuestionById(
            @PathVariable Long id) {

        return ResponseEntity.ok(questionService.getQuestionById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuestionResponse> updateQuestion(
            @PathVariable Long id,
            @RequestBody QuestionRequest request) {

        return ResponseEntity.ok(
                questionService.updateQuestion(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteQuestion(
            @PathVariable Long id) {

        questionService.deleteQuestion(id);

        return ResponseEntity.ok("Question deleted successfully");
    }
}