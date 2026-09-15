package com.campusiq.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusiq.dto.MockTestRequest;
import com.campusiq.dto.MockTestResponse;
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

        return ResponseEntity.ok(
                mockTestService.createMockTest(request)
        );
    }

    @GetMapping
    public ResponseEntity<List<MockTestResponse>> getAllMockTests() {

        return ResponseEntity.ok(
                mockTestService.getAllMockTests()
        );
    }
}