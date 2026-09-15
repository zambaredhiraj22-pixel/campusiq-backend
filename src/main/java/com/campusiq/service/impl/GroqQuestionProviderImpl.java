 package com.campusiq.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@ConditionalOnProperty(
        prefix = "campusiq.ai",
        name = "provider",
        havingValue = "groq"
)
public class GroqQuestionProviderImpl
        implements AiQuestionProvider {

    /*
     * Smaller batches reduce Groq failures and keep the
     * complete 60-question request below proxy time limits.
     */
    private static final int
            MAX_QUESTIONS_PER_REQUEST = 10;

    private static final int
            MAX_GENERATION_ATTEMPTS = 4;

    private static final int
            MAX_RATE_LIMIT_RETRIES = 3;

    private static final int
            MAX_TRANSIENT_RETRIES = 2;

    private static final long
            RATE_LIMIT_WAIT_MILLIS = 15000L;

    private static final long
            REQUEST_GAP_MILLIS = 1500L;

    private static final int
            MAX_LOG_BODY_LENGTH = 1500;

    private final RestClient restClient;

    private final ObjectMapper objectMapper;

    private final String apiKey;

    private final String model;

    private final Object requestLock =
            new Object();

    private long lastRequestTime = 0L;

    public GroqQuestionProviderImpl(
            @Value("${campusiq.ai.groq.api-key:}")
            String apiKey,

            @Value(
                    "${campusiq.ai.groq.model:"
                            + "openai/gpt-oss-20b}"
            )
            String model,

            @Value(
                    "${campusiq.ai.groq.base-url:"
                            + "https://api.groq.com/openai/v1}"
            )
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

        this.restClient =
                RestClient.builder()
                        .baseUrl(
                                normalizeBaseUrl(
                                        baseUrl
                                )
                        )
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

        Map<String, AiGeneratedQuestionResponse>
                uniqueQuestions =
                new LinkedHashMap<>();

        int attempts = 0;

        while (
                uniqueQuestions.size() < count
                &&
                attempts < MAX_GENERATION_ATTEMPTS
        ) {

            attempts++;

            int remaining =
                    count
                            - uniqueQuestions.size();

            int batchSize =
                    Math.min(
                            MAX_QUESTIONS_PER_REQUEST,
                            remaining
                    );

            List<AiGeneratedQuestionResponse>
                    generatedBatch =
                    requestQuestionBatch(
                            category,
                            technicalSkill,
                            batchSize
                    );

            for (
                    AiGeneratedQuestionResponse question
                    : generatedBatch
            ) {

                if (
                        !isQuestionStructurallyValid(
                                question
                        )
                ) {
                    continue;
                }

                String normalizedQuestion =
                        normalizeQuestionKey(
                                question
                                        .getQuestionText()
                        );

                uniqueQuestions.putIfAbsent(
                        normalizedQuestion,
                        question
                );

                if (
                        uniqueQuestions.size()
                                == count
                ) {
                    break;
                }
            }
        }

        if (
                uniqueQuestions.size()
                        != count
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Groq generated only "
                            + uniqueQuestions.size()
                            + " valid unique questions "
                            + "instead of "
                            + count
            );
        }

        return new ArrayList<>(
                uniqueQuestions.values()
        );
    }

    private List<AiGeneratedQuestionResponse>
            requestQuestionBatch(
                    QuestionCategory category,
                    String technicalSkill,
                    int count) {

        Map<String, Object> requestBody =
                buildGenerationRequestBody(
                        category,
                        technicalSkill,
                        count
                );

        try {

            String responseBody =
                    executeGroqRequest(
                            requestBody
                    );

            if (
                    responseBody == null
                    ||
                    responseBody.isBlank()
            ) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Groq returned an empty response"
                );
            }

            String assistantContent =
                    extractAssistantContent(
                            responseBody
                    );

            List<AiGeneratedQuestionResponse>
                    questions =
                    parseGeneratedQuestions(
                            assistantContent,
                            category,
                            technicalSkill
                    );

            if (
                    !isBatchStructurallyValid(
                            questions,
                            count
                    )
            ) {

                return List.of();
            }

            return questions;

        } catch (
                ResourceAccessException ex
        ) {

            log.warn(
                    "Unable to connect to Groq: {}",
                    ex.getMessage()
            );

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to connect to Groq AI"
            );

        } catch (
                ResponseStatusException ex
        ) {

            throw ex;

        } catch (
                Exception ex
        ) {

            log.warn(
                    "Invalid Groq response: {}",
                    ex.getMessage()
            );

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Invalid response received from Groq AI"
            );
        }
    }

    private String executeGroqRequest(
            Map<String, Object> originalRequestBody) {

        Map<String, Object>
                effectiveRequestBody =
                new LinkedHashMap<>(
                        originalRequestBody
                );

        boolean jsonObjectFallbackEnabled =
                false;

        int rateLimitRetries = 0;

        int transientRetries = 0;

        while (true) {

            waitForRequestGap();

            try {

                return restClient
                        .post()
                        .uri(
                                "/chat/completions"
                        )
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + apiKey
                        )
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .body(
                                effectiveRequestBody
                        )
                        .retrieve()
                        .body(
                                String.class
                        );

            } catch (
                    RestClientResponseException ex
            ) {

                int status =
                        ex.getStatusCode()
                                .value();

                String errorBody =
                        ex.getResponseBodyAsString();

                log.warn(
                        "Groq request failed: "
                                + "status={}, model={}, "
                                + "response={}",
                        status,
                        model,
                        sanitizeGroqErrorBody(
                                errorBody
                        )
                );

                /*
                 * If strict JSON Schema is rejected,
                 * retry once using Groq JSON Object mode.
                 */
                if (
                        status == 400
                        &&
                        !jsonObjectFallbackEnabled
                ) {

                    effectiveRequestBody.put(
                            "response_format",
                            Map.of(
                                    "type",
                                    "json_object"
                            )
                    );

                    jsonObjectFallbackEnabled =
                            true;

                    continue;
                }

                if (
                        status == 429
                        &&
                        rateLimitRetries
                                < MAX_RATE_LIMIT_RETRIES
                ) {

                    rateLimitRetries++;

                    sleepSafely(
                            RATE_LIMIT_WAIT_MILLIS
                                    * rateLimitRetries
                    );

                    continue;
                }

                if (
                        status >= 500
                        &&
                        transientRetries
                                < MAX_TRANSIENT_RETRIES
                ) {

                    transientRetries++;

                    sleepSafely(
                            REQUEST_GAP_MILLIS
                                    * transientRetries
                    );

                    continue;
                }

                throw mapGroqError(
                        ex
                );
            }
        }
    }

    private Map<String, Object>
            buildGenerationRequestBody(
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
                "messages",
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
                "response_format",
                buildStructuredOutputFormat()
        );

        requestBody.put(
                "reasoning_effort",
                "low"
        );

        requestBody.put(
                "max_completion_tokens",
                calculateMaxCompletionTokens(
                        count
                )
        );

        return requestBody;
    }

    private String buildSystemPrompt() {

        return """
                You generate high-quality multiple-choice
                placement assessment questions for CAMPUS-IQ.

                Return only a JSON object with this structure:

                {
                  "questions": [
                    {
                      "questionText": "Question text",
                      "optionA": "First option",
                      "optionB": "Second option",
                      "optionC": "Third option",
                      "optionD": "Fourth option",
                      "correctOption": "A"
                    }
                  ]
                }

                Mandatory rules:

                1. Return exactly the requested number of questions.
                2. Every question must be clear and unambiguous.
                3. Every question must have exactly four options.
                4. All four options must be different.
                5. Exactly one option must be correct.
                6. correctOption must be only A, B, C or D.
                7. correctOption must point to the real correct answer.
                8. Do not use "All of the above".
                9. Do not use "None of the above".
                10. Do not create image-dependent questions.
                11. Do not create questions with missing information.
                12. Do not include markdown or explanations.
                13. Avoid duplicate questions.
                14. Keep question and option text concise.
                15. Verify the answer internally before returning it.

                For aptitude questions, calculate the exact
                mathematical answer before creating the options.

                For reasoning questions, independently solve
                the logic before selecting correctOption.

                For technical questions, verify the technical
                fact before selecting correctOption.
                """;
    }

    private String buildGenerationPrompt(
            QuestionCategory category,
            String technicalSkill,
            int count) {

        if (
                category
                        == QuestionCategory.APTITUDE
        ) {

            return """
                    Generate exactly %d unique aptitude
                    placement questions.

                    Cover a balanced selection of:

                    - percentages
                    - ratios and proportions
                    - averages
                    - profit and loss
                    - time and work
                    - time, speed and distance
                    - simple and compound interest
                    - number systems
                    - probability
                    - data interpretation

                    Use placement-test difficulty.

                    Return only the required JSON object.
                    """
                    .formatted(
                            count
                    );
        }

        if (
                category
                        == QuestionCategory.REASONING
        ) {

            return """
                    Generate exactly %d unique logical
                    reasoning placement questions.

                    Cover a balanced selection of:

                    - number and letter series
                    - coding and decoding
                    - blood relations
                    - directions
                    - syllogisms
                    - analogies
                    - classification
                    - logical arrangements
                    - statement and conclusion
                    - pattern recognition

                    Use placement-test difficulty.

                    Return only the required JSON object.
                    """
                    .formatted(
                            count
                    );
        }

        return """
                Generate exactly %d unique technical
                placement questions for this skill:

                %s

                Questions must test practical understanding,
                core concepts, debugging knowledge and
                interview-level technical fundamentals.

                Every question must belong only to the
                specified technical skill.

                Return only the required JSON object.
                """
                .formatted(
                        count,
                        technicalSkill
                );
    }

    private Map<String, Object>
            buildStructuredOutputFormat() {

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
                questionsArray =
                new LinkedHashMap<>();

        questionsArray.put(
                "type",
                "array"
        );

        questionsArray.put(
                "items",
                questionSchema
        );

        Map<String, Object>
                rootProperties =
                new LinkedHashMap<>();

        rootProperties.put(
                "questions",
                questionsArray
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
                jsonSchema =
                new LinkedHashMap<>();

        jsonSchema.put(
                "name",
                "campusiq_generated_questions"
        );

        jsonSchema.put(
                "strict",
                true
        );

        jsonSchema.put(
                "schema",
                rootSchema
        );

        Map<String, Object>
                responseFormat =
                new LinkedHashMap<>();

        responseFormat.put(
                "type",
                "json_schema"
        );

        responseFormat.put(
                "json_schema",
                jsonSchema
        );

        return responseFormat;
    }

    private String extractAssistantContent(
            String responseBody)
            throws Exception {

        JsonNode root =
                objectMapper.readTree(
                        responseBody
                );

        JsonNode choices =
                root.get(
                        "choices"
                );

        if (
                choices == null
                ||
                !choices.isArray()
                ||
                choices.isEmpty()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Groq response does not contain choices"
            );
        }

        JsonNode message =
                choices.get(0)
                        .get(
                                "message"
                        );

        if (message == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Groq response does not contain a message"
            );
        }

        JsonNode content =
                message.get(
                        "content"
                );

        if (
                content == null
                ||
                !content.isTextual()
                ||
                content.asText().isBlank()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Groq response does not contain question data"
            );
        }

        return cleanJsonContent(
                content.asText()
        );
    }

    private String cleanJsonContent(
            String content) {

        String cleaned =
                content.trim();

        if (
                cleaned.startsWith(
                        "```json"
                )
        ) {

            cleaned =
                    cleaned.substring(
                            7
                    );
        } else if (
                cleaned.startsWith(
                        "```"
                )
        ) {

            cleaned =
                    cleaned.substring(
                            3
                    );
        }

        if (
                cleaned.endsWith(
                        "```"
                )
        ) {

            cleaned =
                    cleaned.substring(
                            0,
                            cleaned.length() - 3
                    );
        }

        return cleaned.trim();
    }

    private List<AiGeneratedQuestionResponse>
            parseGeneratedQuestions(
                    String content,
                    QuestionCategory category,
                    String technicalSkill)
            throws Exception {

        JsonNode root =
                objectMapper.readTree(
                        content
                );

        JsonNode questionsNode =
                root.get(
                        "questions"
                );

        if (
                questionsNode == null
                ||
                !questionsNode.isArray()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Groq response does not contain questions array"
            );
        }

        List<AiGeneratedQuestionResponse>
                questions =
                new ArrayList<>();

        for (
                JsonNode node
                : questionsNode
        ) {

            AiGeneratedQuestionResponse question =
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
                            .trim()
                            .toUpperCase(
                                    Locale.ROOT
                            )
            );

            question.setCategory(
                    category
            );

            if (
                    category
                            == QuestionCategory.TECHNICAL
            ) {

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

        return questions;
    }

    private boolean isBatchStructurallyValid(
            List<AiGeneratedQuestionResponse> questions,
            int expectedCount) {

        if (
                questions == null
                ||
                questions.size()
                        != expectedCount
        ) {
            return false;
        }

        Set<String> questionKeys =
                new HashSet<>();

        for (
                AiGeneratedQuestionResponse question
                : questions
        ) {

            if (
                    !isQuestionStructurallyValid(
                            question
                    )
            ) {
                return false;
            }

            String key =
                    normalizeQuestionKey(
                            question
                                    .getQuestionText()
                    );

            if (
                    !questionKeys.add(
                            key
                    )
            ) {
                return false;
            }
        }

        return true;
    }

    private boolean isQuestionStructurallyValid(
            AiGeneratedQuestionResponse question) {

        if (question == null) {
            return false;
        }

        if (
                isBlank(
                        question.getQuestionText()
                )
                ||
                isBlank(
                        question.getOptionA()
                )
                ||
                isBlank(
                        question.getOptionB()
                )
                ||
                isBlank(
                        question.getOptionC()
                )
                ||
                isBlank(
                        question.getOptionD()
                )
                ||
                isBlank(
                        question.getCorrectOption()
                )
        ) {
            return false;
        }

        String correctOption =
                question
                        .getCorrectOption()
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        if (
                !List.of(
                        "A",
                        "B",
                        "C",
                        "D"
                ).contains(
                        correctOption
                )
        ) {
            return false;
        }

        question.setCorrectOption(
                correctOption
        );

        List<String> options =
                List.of(
                        question.getOptionA(),
                        question.getOptionB(),
                        question.getOptionC(),
                        question.getOptionD()
                );

        Set<String> normalizedOptions =
                new HashSet<>();

        for (String option : options) {

            String normalizedOption =
                    normalizeOptionKey(
                            option
                    );

            if (
                    normalizedOption.isBlank()
                    ||
                    normalizedOption.equals(
                            "all of the above"
                    )
                    ||
                    normalizedOption.equals(
                            "none of the above"
                    )
                    ||
                    !normalizedOptions.add(
                            normalizedOption
                    )
            ) {
                return false;
            }
        }

        return true;
    }

    private String getRequiredText(
            JsonNode node,
            String fieldName) {

        JsonNode field =
                node.get(
                        fieldName
                );

        if (
                field == null
                ||
                !field.isTextual()
                ||
                field.asText().isBlank()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Groq returned invalid field: "
                            + fieldName
            );
        }

        return field
                .asText()
                .trim();
    }

    private void waitForRequestGap() {

        synchronized (requestLock) {

            long currentTime =
                    System.currentTimeMillis();

            long elapsed =
                    currentTime
                            - lastRequestTime;

            long remaining =
                    REQUEST_GAP_MILLIS
                            - elapsed;

            if (remaining > 0) {

                sleepSafely(
                        remaining
                );
            }

            lastRequestTime =
                    System.currentTimeMillis();
        }
    }

    private void sleepSafely(
            long milliseconds) {

        try {

            Thread.sleep(
                    milliseconds
            );

        } catch (
                InterruptedException ex
        ) {

            Thread.currentThread()
                    .interrupt();

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI generation was interrupted"
            );
        }
    }

    private String sanitizeGroqErrorBody(
            String responseBody) {

        if (
                responseBody == null
                ||
                responseBody.isBlank()
        ) {
            return "<empty>";
        }

        String sanitized =
                responseBody
                        .replaceAll(
                                "(?i)gsk_[a-zA-Z0-9_-]+",
                                "[REDACTED_GROQ_KEY]"
                        )
                        .replaceAll(
                                "[\\r\\n\\t]+",
                                " "
                        );

        if (
                sanitized.length()
                        <= MAX_LOG_BODY_LENGTH
        ) {
            return sanitized;
        }

        return sanitized.substring(
                0,
                MAX_LOG_BODY_LENGTH
        ) + "...";
    }

    private ResponseStatusException
            mapGroqError(
                    RestClientResponseException ex) {

        int status =
                ex.getStatusCode()
                        .value();

        if (status == 400) {

            return new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Groq rejected the AI generation request"
            );
        }

        if (
                status == 401
                ||
                status == 403
        ) {

            return new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Groq API credentials are invalid or unauthorized"
            );
        }

        if (status == 404) {

            return new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Configured Groq model is unavailable"
            );
        }

        if (status == 429) {

            return new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Groq API rate limit was reached. "
                            + "Please try again later."
            );
        }

        if (status >= 500) {

            return new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Groq AI service is temporarily unavailable"
            );
        }

        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Groq API request failed with HTTP status "
                        + status
        );
    }

    private void validateProviderConfiguration() {

        if (apiKey.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Groq API key is not configured"
            );
        }

        if (model.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Groq AI model is not configured"
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

        if (
                category
                        == QuestionCategory.TECHNICAL
                &&
                (
                        technicalSkill == null
                        ||
                        technicalSkill.isBlank()
                )
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Technical skill is required"
            );
        }
    }

    private int calculateMaxCompletionTokens(
            int count) {

        return Math.max(
                1600,
                count * 240
        );
    }

    private boolean isBlank(
            String value) {

        return value == null
                ||
                value.isBlank();
    }

    private String normalizeQuestionKey(
            String questionText) {

        return questionText
                .trim()
                .toLowerCase(
                        Locale.ROOT
                )
                .replaceAll(
                        "\\s+",
                        " "
                );
    }

    private String normalizeOptionKey(
            String option) {

        return option
                .trim()
                .toLowerCase(
                        Locale.ROOT
                )
                .replaceAll(
                        "\\s+",
                        " "
                );
    }

    private String normalizeBaseUrl(
            String baseUrl) {

        if (
                baseUrl == null
                ||
                baseUrl.isBlank()
        ) {

            return "https://api.groq.com/openai/v1";
        }

        String normalizedBaseUrl =
                baseUrl.trim();

        while (
                normalizedBaseUrl.endsWith(
                        "/"
                )
        ) {

            normalizedBaseUrl =
                    normalizedBaseUrl.substring(
                            0,
                            normalizedBaseUrl.length()
                                    - 1
                    );
        }

        return normalizedBaseUrl;
    }
}