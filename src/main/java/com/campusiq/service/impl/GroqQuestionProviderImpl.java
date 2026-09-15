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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@ConditionalOnProperty(
        prefix = "campusiq.ai",
        name = "provider",
        havingValue = "groq"
)
public class GroqQuestionProviderImpl
        implements AiQuestionProvider {

    private static final int MAX_QUESTIONS_PER_REQUEST = 15;

    private static final int MAX_GENERATION_ATTEMPTS = 8;

    private static final int MAX_RATE_LIMIT_RETRIES = 3;

    private static final long RATE_LIMIT_WAIT_MILLIS = 15000L;

    private static final long REQUEST_GAP_MILLIS = 2000L;

    private final RestClient restClient;

    private final ObjectMapper objectMapper;

    private final String apiKey;

    private final String model;

    private final Object requestLock = new Object();

    private long lastRequestTime = 0L;

    public GroqQuestionProviderImpl(
            @Value("${campusiq.ai.groq.api-key:}")
            String apiKey,

            @Value("${campusiq.ai.groq.model:openai/gpt-oss-20b}")
            String model,

            @Value("${campusiq.ai.groq.base-url:https://api.groq.com/openai/v1}")
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
                                normalizeBaseUrl(baseUrl)
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

            int remaining =
                    count - uniqueQuestions.size();

            int batchSize =
                    Math.min(
                            MAX_QUESTIONS_PER_REQUEST,
                            remaining
                    );

            List<AiGeneratedQuestionResponse>
                    batch =
                    generateVerifiedBatch(
                            category,
                            technicalSkill,
                            batchSize
                    );

            if (batch.isEmpty()) {
                attempts++;
                continue;
            }

            for (
                    AiGeneratedQuestionResponse question
                    : batch
            ) {

                if (
                        !isQuestionStructurallyValid(
                                question
                        )
                ) {
                    continue;
                }

                String key =
                        normalizeQuestionKey(
                                question.getQuestionText()
                        );

                uniqueQuestions.putIfAbsent(
                        key,
                        question
                );

                if (
                        uniqueQuestions.size()
                                >= count
                ) {
                    break;
                }
            }

            attempts++;
        }

        if (
                uniqueQuestions.size()
                        != count
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Groq could generate and verify only "
                            + uniqueQuestions.size()
                            + " valid unique questions instead of "
                            + count
            );
        }

        return new ArrayList<>(
                uniqueQuestions.values()
        );
    }

    private List<AiGeneratedQuestionResponse>
            generateVerifiedBatch(
                    QuestionCategory category,
                    String technicalSkill,
                    int count) {

        List<AiGeneratedQuestionResponse>
                generated =
                requestQuestionBatch(
                        buildGenerationRequestBody(
                                category,
                                technicalSkill,
                                count
                        ),
                        category,
                        technicalSkill
                );

        if (
                !isBatchStructurallyValid(
                        generated,
                        count
                )
        ) {
            return List.of();
        }

        List<AiGeneratedQuestionResponse>
                verified =
                requestQuestionBatch(
                        buildVerificationRequestBody(
                                generated,
                                category,
                                technicalSkill
                        ),
                        category,
                        technicalSkill
                );

        if (
                !isBatchStructurallyValid(
                        verified,
                        count
                )
        ) {
            return List.of();
        }

        return verified;
    }

    private List<AiGeneratedQuestionResponse>
            requestQuestionBatch(
                    Map<String, Object> requestBody,
                    QuestionCategory category,
                    String technicalSkill) {

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

            String content =
                    extractAssistantContent(
                            responseBody
                    );

            return parseGeneratedQuestions(
                    content,
                    category,
                    technicalSkill
            );

        } catch (
                ResourceAccessException ex
        ) {

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

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Invalid response received from Groq AI"
            );
        }
    }

    private String executeGroqRequest(
            Map<String, Object> requestBody) {

        for (
                int attempt = 0;
                attempt <= MAX_RATE_LIMIT_RETRIES;
                attempt++
        ) {

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
                                requestBody
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

                if (
                        status == 429
                        &&
                        attempt < MAX_RATE_LIMIT_RETRIES
                ) {

                    long waitMillis =
                            RATE_LIMIT_WAIT_MILLIS
                                    * (attempt + 1L);

                    sleepSafely(
                            waitMillis
                    );

                    continue;
                }

                throw mapGroqError(
                        ex
                );
            }
        }

        throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Groq API rate limit was reached after automatic retries"
        );
    }

    private void waitForRequestGap() {

        synchronized (requestLock) {

            long now =
                    System.currentTimeMillis();

            long elapsed =
                    now - lastRequestTime;

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

    private Map<String, Object>
            buildGenerationRequestBody(
                    QuestionCategory category,
                    String technicalSkill,
                    int count) {

        Map<String, Object>
                requestBody =
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
                                buildGenerationSystemPrompt()
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

    private Map<String, Object>
            buildVerificationRequestBody(
                    List<AiGeneratedQuestionResponse> questions,
                    QuestionCategory category,
                    String technicalSkill) {

        Map<String, Object>
                requestBody =
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
                                buildVerificationSystemPrompt()
                        ),
                        Map.of(
                                "role",
                                "user",
                                "content",
                                buildVerificationPrompt(
                                        questions,
                                        category,
                                        technicalSkill
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
                "medium"
        );

        requestBody.put(
                "max_completion_tokens",
                calculateMaxCompletionTokens(
                        questions.size()
                )
        );

        return requestBody;
    }

    private String buildGenerationSystemPrompt() {

        return """
                You generate multiple-choice questions
                for the CAMPUS-IQ college placement platform.

                Every question must:

                - be clear and unambiguous
                - have exactly four different options
                - have exactly one correct answer
                - use correctOption only as A, B, C or D
                - contain the real correct answer in exactly one option
                - have correctOption point to that exact option
                - avoid duplicate questions
                - avoid duplicate options
                - avoid all-of-the-above
                - avoid none-of-the-above
                - avoid missing-information questions
                - avoid image-based questions
                - avoid misleading questions
                - avoid markdown
                - contain no explanation outside the structured response

                Before returning a question,
                solve or verify it internally.

                For mathematical questions,
                calculate the exact answer first
                and then create the four options.

                Never return a mathematical question
                if its real answer is absent
                from the options.

                For technical questions,
                verify the technical fact before
                choosing correctOption.

                Keep question wording concise.

                Keep each option concise.

                Return exactly the requested number
                of questions.
                """;
    }

    private String buildVerificationSystemPrompt() {

        return """
                You are the quality-control verifier
                for CAMPUS-IQ assessment questions.

                Do not trust the existing correctOption.

                Independently solve or verify
                every supplied question.

                For each question:

                - verify the question is valid
                - verify enough information is provided
                - verify all four options are different
                - verify exactly one option is correct
                - verify the actual answer is present
                - verify correctOption points to the actual answer

                For aptitude:
                recalculate the answer from scratch.

                For reasoning:
                solve the logic independently.

                For technical questions:
                independently verify the technical fact.

                If the question is invalid,
                mathematically incorrect,
                ambiguous,
                outdated,
                or the real answer is missing,
                replace it with a new valid question
                of the same category.

                If only the answer key or options
                are wrong, correct them.

                Return exactly the same number
                of questions.

                Return only structured question data.

                Do not return explanations.
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
                    Generate exactly %d unique
                    quantitative aptitude MCQs
                    for campus placements.

                    Use a balanced mixture of:

                    percentages,
                    profit and loss,
                    ratio and proportion,
                    averages,
                    time and work,
                    time speed and distance,
                    simple and compound interest,
                    probability,
                    number systems,
                    quantitative reasoning.

                    For every question:

                    solve it first,
                    calculate the exact result,
                    place that result in exactly one option,
                    create three incorrect distractors,
                    recheck the calculation,
                    then set correctOption.

                    Prefer integer or simple decimal answers.

                    If rounding is needed,
                    state the rounding rule.

                    Never create a question
                    whose actual answer is absent
                    from the options.

                    Keep each question reasonably short.

                    Return exactly %d questions.
                    """
                    .formatted(
                            count,
                            count
                    );
        }

        if (
                category
                        == QuestionCategory.REASONING
        ) {

            return """
                    Generate exactly %d unique
                    logical reasoning MCQs
                    for campus placements.

                    Use a balanced mixture of:

                    number series,
                    alphabet series,
                    coding and decoding,
                    syllogisms,
                    directions,
                    blood relations,
                    analogies,
                    logical sequences,
                    classification,
                    statement reasoning.

                    Solve each question internally.

                    Ensure exactly one option
                    is logically correct.

                    Do not create questions
                    with insufficient information.

                    Avoid visual puzzles.

                    Keep each question reasonably short.

                    Return exactly %d questions.
                    """
                    .formatted(
                            count,
                            count
                    );
        }

        return """
                Generate exactly %d unique
                technical MCQs for campus placements.

                Technical skill:
                %s

                Questions must be specifically
                related to %s.

                Test:

                fundamentals,
                concepts,
                syntax,
                behavior,
                practical knowledge,
                interview knowledge,
                problem solving.

                Verify every technical fact
                before selecting correctOption.

                Avoid obsolete or
                version-dependent claims
                unless the version is stated.

                Ensure exactly one option
                is correct.

                Keep questions concise.

                Return exactly %d questions.
                """
                .formatted(
                        count,
                        technicalSkill,
                        technicalSkill,
                        count
                );
    }

    private String buildVerificationPrompt(
            List<AiGeneratedQuestionResponse> questions,
            QuestionCategory category,
            String technicalSkill) {

        try {

            List<Map<String, String>>
                    inputQuestions =
                    new ArrayList<>();

            for (
                    AiGeneratedQuestionResponse question
                    : questions
            ) {

                Map<String, String>
                        value =
                        new LinkedHashMap<>();

                value.put(
                        "questionText",
                        question.getQuestionText()
                );

                value.put(
                        "optionA",
                        question.getOptionA()
                );

                value.put(
                        "optionB",
                        question.getOptionB()
                );

                value.put(
                        "optionC",
                        question.getOptionC()
                );

                value.put(
                        "optionD",
                        question.getOptionD()
                );

                value.put(
                        "correctOption",
                        question.getCorrectOption()
                );

                inputQuestions.add(
                        value
                );
            }

            String json =
                    objectMapper
                            .writeValueAsString(
                                    inputQuestions
                            );

            String skill =
                    category
                            == QuestionCategory.TECHNICAL
                            ? technicalSkill
                            : "Not applicable";

            return """
                    Category:
                    %s

                    Technical skill:
                    %s

                    Independently verify these
                    %d questions.

                    Do not trust the supplied
                    correctOption.

                    Fix or replace every invalid
                    question.

                    Preserve the category.

                    For technical questions,
                    preserve the technical skill.

                    Return exactly %d
                    verified questions.

                    Questions:

                    %s
                    """
                    .formatted(
                            category.name(),
                            skill,
                            questions.size(),
                            questions.size(),
                            json
                    );

        } catch (
                Exception ex
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to prepare AI question verification request"
            );
        }
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

        JsonNode firstChoice =
                choices.get(
                        0
                );

        JsonNode message =
                firstChoice.get(
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
                    "Groq response does not contain generated question data"
            );
        }

        return content
                .asText()
                .trim();
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
                    "Groq response does not contain a questions array"
            );
        }

        List<AiGeneratedQuestionResponse>
                questions =
                new ArrayList<>();

        for (
                JsonNode node
                : questionsNode
        ) {

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

        Set<String>
                questionKeys =
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
                            question.getQuestionText()
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

        List<String>
                options =
                List.of(
                        question.getOptionA(),
                        question.getOptionB(),
                        question.getOptionC(),
                        question.getOptionD()
                );

        Set<String>
                normalizedOptions =
                new HashSet<>();

        for (
                String option
                : options
        ) {

            String normalized =
                    normalizeOptionKey(
                            option
                    );

            if (
                    normalized.isBlank()
                    ||
                    normalized.equals(
                            "all of the above"
                    )
                    ||
                    normalized.equals(
                            "none of the above"
                    )
            ) {
                return false;
            }

            if (
                    !normalizedOptions.add(
                            normalized
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

        JsonNode value =
                node.get(
                        fieldName
                );

        if (
                value == null
                ||
                !value.isTextual()
                ||
                value.asText().isBlank()
        ) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Groq returned invalid field: "
                            + fieldName
            );
        }

        return value
                .asText()
                .trim();
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

    private ResponseStatusException
            mapGroqError(
                    RestClientResponseException ex) {

        int status =
                ex.getStatusCode()
                        .value();

        if (status == 400) {

            return new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Groq rejected the AI generation or verification request"
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
                    "Groq API rate limit was reached after automatic retries. Please try again later."
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

    private int calculateMaxCompletionTokens(
            int count) {

        return Math.max(
                1800,
                count * 260
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

        String value =
                baseUrl.trim();

        while (
                value.endsWith(
                        "/"
                )
        ) {

            value =
                    value.substring(
                            0,
                            value.length() - 1
                    );
        }

        return value;
    }
}