package com.campusiq.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.campusiq.entity.Question;
import com.campusiq.enums.QuestionCategory;

public interface QuestionRepository
        extends JpaRepository<Question, Long> {

    List<Question> findByCategory(
            QuestionCategory category);

    List<Question> findByCategoryAndTechnicalSkillIgnoreCase(
            QuestionCategory category,
            String technicalSkill);

    boolean existsByQuestionTextIgnoreCase(
            String questionText);

    boolean existsByQuestionTextIgnoreCaseAndIdNot(
            String questionText,
            Long id);
}