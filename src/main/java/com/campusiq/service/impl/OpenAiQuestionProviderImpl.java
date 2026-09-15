package com.campusiq.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import com.campusiq.dto.AiGeneratedQuestionResponse;
import com.campusiq.enums.QuestionCategory;
import com.campusiq.service.AiQuestionProvider;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@ConditionalOnProperty(
        prefix = "campusiq.ai",
        name = "provider",
        havingValue = "openai"
)
public class OpenAiQuestionProviderImpl
        implements AiQuestionProvider {

    private final RestClient restClient;

    private final ObjectMapper objectMapper;

    private final String apiKey;

    private final String model;

    public OpenAiQuestionProviderImpl(

            @Value("${campusiq.ai.openai.api-key:}")
            String apiKey,

            @Value("${campusiq.ai.openai.model:gpt-5.6-luna}")
            String model,

            @Value("${campusiq.ai.openai.base-url:https://api.openai.com/v1}")
            String baseUrl,

            ObjectMapper objectMapper) {

        this.apiKey =
                apiKey == null
                        ? ""
                        : apiKey.trim();

        this.model =
                model == null
                        ? ""
                        : model.trim();

        this.objectMapper =
                objectMapper;

        String normalizedBaseUrl =
                normalizeBaseUrl(baseUrl);

        this.restClient =
                RestClient.builder()
                        .baseUrl(normalizedBaseUrl)
                        .build();
    }

    @Override
    public List<AiGeneratedQuestionResponse>
            generateQuestions(
                    QuestionCategory category,
                    String technicalSkill,
                    int count) {

        validateProviderConfiguration();

        validateGenerationRequest(
                category,
                technicalSkill,
                count
        );

        Map<String, Object> requestBody =
                buildRequestBody(
                        category,
                        technicalSkill,
                        count
                );

        try {

            String responseBody =
                    restClient
                            .post()
                            .uri("/responses")
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer " + apiKey
                            )
                            .contentType(
                                    MediaType.APPLICATION_JSON
                            )
                            .body(requestBody)
                            .retrieve()
                            .body(String.class);

            if (responseBody == null ||
                    responseBody.isBlank()) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "OpenAI returned an empty response"
                );
            }

            String outputText =
                    extractOutputText(
                            responseBody
                    );

            return parseGeneratedQuestions(
                    outputText,
                    category,
                    technicalSkill,
                    count
            );

        } catch (RestClientResponseException ex) {

            throw mapOpenAiError(ex);

        } catch (ResourceAccessException ex) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to connect to the AI provider"
            );

        } catch (ResponseStatusException ex) {

            throw ex;

        } catch (Exception ex) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Invalid response received from the AI provider"
            );
        }
    }

    private Map<String, Object>
            buildRequestBody(
                    QuestionCategory category,
                    String technicalSkill,
                    int count) {

        Map<String, Object> requestBody =
                new LinkedHashMap<>();

        requestBody.put(
                "model",
                model
        );

        requestBody.put(
                "store",
                false
        );

        requestBody.put(
                "input",
                List.of(
                        Map.of(
                                "role",
                                "system",
                                "content",
                                buildSystemPrompt()
                        ),

                        Map.of(
                                "role",
                                "user",
                                "content",
                                buildGenerationPrompt(
                                        category,
                                        technicalSkill,
                                        count
                                )
                        )
                )
        );

        requestBody.put(
                "text",
                Map.of(
                        "format",
                        buildStructuredOutputFormat(
                                count
                        )
                )
        );

        requestBody.put(
                "max_output_tokens",
                calculateMaxOutputTokens(
                        count
                )
        );

        return requestBody;
    }

    private String buildSystemPrompt() {

        return """
                You are the AI question-generation engine
                for CAMPUS-IQ, a college placement
                preparation platform.

                Generate professional placement-level
                multiple-choice questions.

                Every question must:

                - be clear and unambiguous
                - have exactly four different options
                - have exactly one correct answer
                - use correctOption only as A, B, C or D
                - be suitable for college placement preparation
                - avoid duplicate questions
                - avoid duplicate answer options
                - avoid trick questions with unclear answers
                - avoid questions that require images
                - avoid markdown formatting
                - avoid explanations outside the requested data

                Faculty will review all generated questions
                before they are added to the official
                Question Bank.
                """;
    }

    private String buildGenerationPrompt(
            QuestionCategory category,
            String technicalSkill,
            int count) {

        if (category == QuestionCategory.APTITUDE) {

            return """
                    Generate exactly %d unique
                    quantitative aptitude MCQs for
                    campus placement preparation.

                    Use a balanced mixture of topics such as:

                    percentages,
                    profit and loss,
                    ratio and proportion,
                    averages,
                    time and work,
                    time speed and distance,
                    simple and compound interest,
                    probability,
                    number systems,
                    basic quantitative reasoning.

                    Use a reasonable mixture of
                    easy, medium and difficult questions.

                    Do not require diagrams or images.
                    """
                    .formatted(count);
        }

        if (category == QuestionCategory.REASONING) {

            return """
                    Generate exactly %d unique
                    logical reasoning MCQs for
                    campus placement preparation.

                    Use a balanced mixture of topics such as:

                    number series,
                    alphabet series,
                    coding and decoding,
                    syllogisms,
                    directions,
                    blood relations,
                    analogies,
                    logical sequences,
                    classification,
                    statement-based reasoning.

                    Use a reasonable mixture of
                    easy, medium and difficult questions.

                    Avoid questions requiring diagrams,
                    large tables or visual puzzles.
                    """
                    .formatted(count);
        }

        return """
                Generate exactly %d unique technical
                MCQs for campus placement preparation.

                Technical skill:
                %s

                Questions must test practical
                interview-relevant fundamentals,
                concepts, syntax, behavior and
                problem-solving knowledge related
                specifically to %s.

                Use a balanced mixture of
                easy, medium and difficult questions.

                Do not generate questions unrelated
                to the specified technical skill.
                """
                .formatted(
                        count,
                        technicalSkill,
                        technicalSkill
                );
    }

    private Map<String, Object>
            buildStructuredOutputFormat(
                    int count) {

        Map<String, Object>
                questionProperties =
                new LinkedHashMap<>();

        questionProperties.put(
                "questionText",
                Map.of(
                        "type",
                        "string"
                )
        );

        questionProperties.put(
                "optionA",
                Map.of(
                        "type",
                        "string"
                )
        );

        questionProperties.put(
                "optionB",
                Map.of(
                        "type",
                        "string"
                )
        );

        questionProperties.put(
                "optionC",
                Map.of(
                        "type",
                        "string"
                )
        );

        questionProperties.put(
                "optionD",
                Map.of(
                        "type",
                        "string"
                )
        );

        questionProperties.put(
                "correctOption",
                Map.of(
                        "type",
                        "string",
                        "enum",
                        List.of(
                                "A",
                                "B",
                                "C",
                                "D"
                        )
                )
        );

        Map<String, Object>
                questionSchema =
                new LinkedHashMap<>();

        questionSchema.put(
                "type",
                "object"
        );

        questionSchema.put(
                "properties",
                questionProperties
        );

        questionSchema.put(
                "required",
                List.of(
                        "questionText",
                        "optionA",
                        "optionB",
                        "optionC",
                        "optionD",
                        "correctOption"
                )
        );

        questionSchema.put(
                "additionalProperties",
                false
        );

        Map<String, Object>
                questionArraySchema =
                new LinkedHashMap<>();

        questionArraySchema.put(
                "type",
                "array"
        );

        questionArraySchema.put(
                "items",
                questionSchema
        );

        questionArraySchema.put(
                "minItems",
                count
        );

        questionArraySchema.put(
                "maxItems",
                count
        );

        Map<String, Object>
                rootProperties =
                new LinkedHashMap<>();

        rootProperties.put(
                "questions",
                questionArraySchema
        );

        Map<String, Object>
                rootSchema =
                new LinkedHashMap<>();

        rootSchema.put(
                "type",
                "object"
        );

        rootSchema.put(
                "properties",
                rootProperties
        );

        rootSchema.put(
                "required",
                List.of(
                        "questions"
                )
        );

        rootSchema.put(
                "additionalProperties",
                false
        );

        Map<String, Object>
                format =
                new LinkedHashMap<>();

        format.put(
                "type",
                "json_schema"
        );

        format.put(
                "name",
                "campusiq_generated_questions"
        );

        format.put(
                "strict",
                true
        );

        format.put(
                "schema",
                rootSchema
        );

        return format;
    }

    private String extractOutputText(
            String responseBody)
            throws Exception {

        JsonNode root =
                objectMapper.readTree(
                        responseBody
                );

        JsonNode output =
                root.get("output");

        if (output == null ||
                !output.isArray()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI response does not contain output"
            );
        }

        for (JsonNode outputItem : output) {

            JsonNode content =
                    outputItem.get(
                            "content"
                    );

            if (content == null ||
                    !content.isArray()) {

                continue;
            }

            for (JsonNode contentItem
                    : content) {

                JsonNode typeNode =
                        contentItem.get(
                                "type"
                        );

                if (typeNode != null &&
                        "output_text".equals(
                                typeNode.asText()
                        )) {

                    JsonNode textNode =
                            contentItem.get(
                                    "text"
                            );

                    if (textNode != null &&
                            textNode.isTextual() &&
                            !textNode.asText()
                                    .isBlank()) {

                        return textNode
                                .asText();
                    }
                }

                JsonNode refusalNode =
                        contentItem.get(
                                "refusal"
                        );

                if (refusalNode != null &&
                        refusalNode.isTextual() &&
                        !refusalNode.asText()
                                .isBlank()) {

                    throw new ResponseStatusException(
                            HttpStatus.BAD_GATEWAY,
                            "AI provider refused question generation"
                    );
                }
            }
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "AI response does not contain generated question data"
        );
    }

    private List<AiGeneratedQuestionResponse>
            parseGeneratedQuestions(
                    String outputText,
                    QuestionCategory category,
                    String technicalSkill,
                    int expectedCount)
            throws Exception {

        JsonNode root =
                objectMapper.readTree(
                        outputText
                );

        JsonNode questionsNode =
                root.get(
                        "questions"
                );

        if (questionsNode == null ||
                !questionsNode.isArray()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI response does not contain a questions array"
            );
        }

        List<AiGeneratedQuestionResponse>
                questions =
                new ArrayList<>();

        for (JsonNode node
                : questionsNode) {

            AiGeneratedQuestionResponse
                    question =
                    new AiGeneratedQuestionResponse();

            question.setQuestionText(
                    getRequiredText(
                            node,
                            "questionText"
                    )
            );

            question.setOptionA(
                    getRequiredText(
                            node,
                            "optionA"
                    )
            );

            question.setOptionB(
                    getRequiredText(
                            node,
                            "optionB"
                    )
            );

            question.setOptionC(
                    getRequiredText(
                            node,
                            "optionC"
                    )
            );

            question.setOptionD(
                    getRequiredText(
                            node,
                            "optionD"
                    )
            );

            question.setCorrectOption(
                    getRequiredText(
                            node,
                            "correctOption"
                    )
            );

            question.setCategory(
                    category
            );

            if (category
                    == QuestionCategory.TECHNICAL) {

                question.setTechnicalSkill(
                        technicalSkill.trim()
                );

            } else {

                question.setTechnicalSkill(
                        null
                );
            }

            questions.add(
                    question
            );
        }

        if (questions.size()
                != expectedCount) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI returned "
                            + questions.size()
                            + " questions instead of "
                            + expectedCount
            );
        }

        return questions;
    }

    private String getRequiredText(
            JsonNode node,
            String fieldName) {

        JsonNode value =
                node.get(
                        fieldName
                );

        if (value == null ||
                !value.isTextual() ||
                value.asText().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI returned invalid field: "
                            + fieldName
            );
        }

        return value
                .asText()
                .trim();
    }

    private void validateProviderConfiguration() {

        if (apiKey.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "OpenAI API key is not configured"
            );
        }

        if (model.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "OpenAI model is not configured"
            );
        }
    }

    private void validateGenerationRequest(
            QuestionCategory category,
            String technicalSkill,
            int count) {

        if (category == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Question category is required"
            );
        }

        if (count <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Question count must be greater than zero"
            );
        }

        if (category
                == QuestionCategory.TECHNICAL &&
                (technicalSkill == null ||
                        technicalSkill.isBlank())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Technical skill is required"
            );
        }
    }

    private ResponseStatusException
            mapOpenAiError(
                    RestClientResponseException ex) {

        int status =
                ex.getStatusCode()
                        .value();

        if (status == 401 ||
                status == 403) {

            return new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "OpenAI API credentials are invalid or unauthorized"
            );
        }

        if (status == 429) {

            return new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "OpenAI API rate limit or quota was reached"
            );
        }

        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "OpenAI API request failed with HTTP status "
                        + status
        );
    }

    private int calculateMaxOutputTokens(
            int count) {

        return Math.max(
                4000,
                count * 500
        );
    }

    private String normalizeBaseUrl(
            String baseUrl) {

        if (baseUrl == null ||
                baseUrl.isBlank()) {

            return "https://api.openai.com/v1";
        }

        String value =
                baseUrl.trim();

        while (value.endsWith("/")) {

            value =
                    value.substring(
                            0,
                            value.length() - 1
                    );
        }

        return value;
    }
}