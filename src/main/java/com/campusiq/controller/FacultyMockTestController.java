package com.campusiq.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusiq.dto.MockTestRequest;
import com.campusiq.dto.MockTestResponse;
import com.campusiq.dto.MockTestRetakeResponse;
import com.campusiq.service.MockTestService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/faculty/mock-tests")
@RequiredArgsConstructor
public class FacultyMockTestController {

    private final MockTestService mockTestService;

    @PostMapping
    public ResponseEntity<MockTestResponse> createMockTest(
            @RequestBody MockTestRequest request) {
        MockTestResponse response =
                mockTestService.createMockTest(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<MockTestResponse>>
    getAllMockTests() {
        return ResponseEntity.ok(
                mockTestService.getAllMockTests()
        );
    }

    @GetMapping("/retakes")
    public ResponseEntity<List<MockTestRetakeResponse>>
    getRetakeStatuses() {
        return ResponseEntity.ok(
                mockTestService.getRetakeStatuses()
        );
    }

    @PostMapping(
            "/assignments/{assignmentId}/allow-retake"
    )
    public ResponseEntity<MockTestRetakeResponse>
    allowRetake(
            @PathVariable("assignmentId")
            Long assignmentId) {
        return ResponseEntity.ok(
                mockTestService.allowRetake(
                        assignmentId
                )
        );
    }
}