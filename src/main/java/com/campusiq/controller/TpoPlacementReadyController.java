package com.campusiq.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusiq.dto.TpoPlacementReadyStudentResponse;
import com.campusiq.service.TpoPlacementReadyService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tpo/placement-ready-students")
@RequiredArgsConstructor
public class TpoPlacementReadyController {

    private final TpoPlacementReadyService
            tpoPlacementReadyService;

    @GetMapping
    public ResponseEntity<
            List<TpoPlacementReadyStudentResponse>>
            getPlacementReadyStudents() {

        return ResponseEntity.ok(
                tpoPlacementReadyService
                        .getPlacementReadyStudents()
        );
    }
}