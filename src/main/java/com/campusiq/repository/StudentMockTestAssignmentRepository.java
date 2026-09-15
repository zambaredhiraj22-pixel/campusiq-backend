package com.campusiq.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.campusiq.entity.StudentMockTestAssignment;

public interface StudentMockTestAssignmentRepository
        extends JpaRepository<StudentMockTestAssignment, Long> {

    /*
     * Student dashboard वर त्या student ला assign
     * केलेले सर्व active personalized mock tests मिळवण्यासाठी.
     *
     * Latest assignment first.
     */
    @EntityGraph(attributePaths = "mockTest")
    List<StudentMockTestAssignment>
            findByStudentProfileIdAndActiveTrueOrderByAssignedAtDesc(
                    Long studentProfileId
            );

    /*
     * Specific mock test हा त्या student ला
     * खरंच assign झाला आहे का ते securely verify करण्यासाठी.
     *
     * Student test start करताना हा method important आहे.
     */
    @EntityGraph(attributePaths = "mockTest")
    Optional<StudentMockTestAssignment>
            findByStudentProfileIdAndMockTestIdAndActiveTrue(
                    Long studentProfileId,
                    Long mockTestId
            );

    /*
     * Quick security/business check.
     *
     * true  = test assigned and active
     * false = student ला हा personalized test access नाही
     */
    boolean existsByStudentProfileIdAndMockTestIdAndActiveTrue(
            Long studentProfileId,
            Long mockTestId
    );

    /*
     * Faculty/backend ला एखाद्या mock test चे
     * assignments पाहण्यासाठी useful.
     */
    @EntityGraph(attributePaths = "studentProfile")
    List<StudentMockTestAssignment>
            findByMockTestIdOrderByAssignedAtDesc(
                    Long mockTestId
            );

    /*
     * Same student + same test यासाठी assignment
     * already exists का हे creation time ला check करण्यासाठी.
     *
     * Active false असला तरी database unique constraint
     * duplicate row allow करणार नाही, म्हणून हा method
     * active condition शिवाय ठेवला आहे.
     */
    Optional<StudentMockTestAssignment>
            findByStudentProfileIdAndMockTestId(
                    Long studentProfileId,
                    Long mockTestId
            );
}