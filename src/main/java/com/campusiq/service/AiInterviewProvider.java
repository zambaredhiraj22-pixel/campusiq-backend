package com.campusiq.service;

import java.util.List;

import com.campusiq.enums.AiInterviewQuestionType;

public interface AiInterviewProvider {

    List<GeneratedInterviewQuestion> generateQuestions(
            String resumeText,
            String projects,
            String technologies,
            List<String> verifiedSkills
    );

    AnswerEvaluation evaluateAnswer(
            String questionText,
            String referenceAnswer,
            String evaluationCriteria,
            String studentAnswer,
            AiInterviewQuestionType questionType
    );

    String generateOverallFeedback(
            List<String> answerFeedbacks
    );

    record GeneratedInterviewQuestion(
            AiInterviewQuestionType questionType,
            String questionText,
            String technicalSkill,
            String referenceAnswer,
            String evaluationCriteria
    ) {
    }

    record AnswerEvaluation(
            double contentScore,
            double relevanceScore,
            double communicationScore,
            double overallScore,
            String feedback
    ) {
    }
}