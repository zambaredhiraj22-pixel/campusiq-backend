package com.campusiq.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusiq.dto.PlacementReadinessResponse;
import com.campusiq.entity.StudentProfile;
import com.campusiq.exception.StudentProfileNotFoundException;
import com.campusiq.repository.StudentProfileRepository;
import com.campusiq.service.PlacementReadinessService;

@RestController
@RequestMapping("/api/readiness")
public class PlacementReadinessController {

    private final PlacementReadinessService placementReadinessService;
    private final StudentProfileRepository studentProfileRepository;

    public PlacementReadinessController(
            PlacementReadinessService placementReadinessService,
            StudentProfileRepository studentProfileRepository) {

        this.placementReadinessService = placementReadinessService;
        this.studentProfileRepository = studentProfileRepository;
    }

    @GetMapping("/{studentProfileId}")
    public ResponseEntity<PlacementReadinessResponse> getPlacementReadiness(
            @PathVariable("studentProfileId") Long studentProfileId,
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (studentProfileId == null || studentProfileId <= 0) {
            return ResponseEntity.badRequest().build();
        }

        boolean staffAccess = hasRole(authentication, "FACULTY")
                || hasRole(authentication, "TPO")
                || hasRole(authentication, "ADMIN");

        if (!staffAccess) {

            if (!hasRole(authentication, "STUDENT")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            StudentProfile ownProfile = studentProfileRepository
                    .findByUserUsername(authentication.getName())
                    .orElseThrow(() -> new StudentProfileNotFoundException(
                            "Please create your student profile first"));

            if (!studentProfileId.equals(ownProfile.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

        } else if (!studentProfileRepository.existsById(studentProfileId)) {

            throw new StudentProfileNotFoundException(
                    "Student profile not found with id: " + studentProfileId);
        }

        PlacementReadinessResponse response = placementReadinessService
                .getPlacementReadiness(studentProfileId);

        return ResponseEntity.ok(response);
    }

    private boolean hasRole(Authentication authentication, String role) {

        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        ("ROLE_" + role).equals(authority.getAuthority()));
    }
}





//package com.campusiq.controller;
//
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import com.campusiq.dto.PlacementReadinessResponse;
//import com.campusiq.service.PlacementReadinessService;
//
//@RestController
//@RequestMapping("/api/readiness")
//public class PlacementReadinessController {
//
//    private final PlacementReadinessService placementReadinessService;
//
//    public PlacementReadinessController(
//            PlacementReadinessService placementReadinessService) {
//
//        this.placementReadinessService =
//                placementReadinessService;
//    }
//
//    @GetMapping("/{studentProfileId}")
//    public ResponseEntity<PlacementReadinessResponse>
//            getPlacementReadiness(
//                    @PathVariable Long studentProfileId) {
//
//        PlacementReadinessResponse response =
//                placementReadinessService
//                        .getPlacementReadiness(
//                                studentProfileId);
//
//        return ResponseEntity.ok(response);
//    }
//}