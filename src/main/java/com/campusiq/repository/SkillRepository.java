package com.campusiq.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campusiq.entity.Skill;
import com.campusiq.entity.StudentProfile;
import com.campusiq.enums.SkillStatus;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    List<Skill> findByStudentProfile(StudentProfile studentProfile);

    boolean existsByStudentProfileAndSkillNameIgnoreCase(
            StudentProfile studentProfile,
            String skillName
    );

    List<Skill> findByStatus(SkillStatus status);

    List<Skill> findByStudentProfileAndStatus(
            StudentProfile studentProfile,
            SkillStatus status
    );

    boolean existsByStudentProfileAndSkillNameIgnoreCaseAndStatus(
            StudentProfile studentProfile,
            String skillName,
            SkillStatus status
    );
}