package com.campusiq.exception;

import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.campusiq.dto.ApiErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(UsernameAlreadyExistsException.class)
	public ResponseEntity<ApiErrorResponse> handleUsernameAlreadyExists(UsernameAlreadyExistsException exception,
			HttpServletRequest request) {

		return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request);
	}

	@ExceptionHandler(PasswordMismatchException.class)
	public ResponseEntity<ApiErrorResponse> handlePasswordMismatch(PasswordMismatchException exception,
			HttpServletRequest request) {

		return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
	}

	@ExceptionHandler(DisabledException.class)
	public ResponseEntity<ApiErrorResponse> handleDisabledAccount(DisabledException exception,
			HttpServletRequest request) {

		return buildResponse(HttpStatus.FORBIDDEN, exception.getMessage(), request);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiErrorResponse> handleAuthenticationException(AuthenticationException exception,
			HttpServletRequest request) {

		return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid username or password", request);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException exception,
			HttpServletRequest request) {

		return buildResponse(HttpStatus.FORBIDDEN, exception.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidationErrors(MethodArgumentNotValidException exception,
			HttpServletRequest request) {

		String message = exception.getBindingResult().getAllErrors().get(0).getDefaultMessage();

		return buildResponse(HttpStatus.BAD_REQUEST, message, request);
	}

	@ExceptionHandler(StudentProfileAlreadyExistsException.class)
	public ResponseEntity<ApiErrorResponse> handleProfileAlreadyExists(StudentProfileAlreadyExistsException exception,
			HttpServletRequest request) {

		return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request);
	}

	@ExceptionHandler(StudentProfileNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleProfileNotFound(StudentProfileNotFoundException exception,
			HttpServletRequest request) {

		return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
	}

	@ExceptionHandler(SkillAlreadyExistsException.class)
	public ResponseEntity<ApiErrorResponse> handleSkillAlreadyExists(SkillAlreadyExistsException exception,
			HttpServletRequest request) {

		return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request);
	}

	@ExceptionHandler(SkillNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleSkillNotFound(SkillNotFoundException exception,
			HttpServletRequest request) {

		return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
	}

	@ExceptionHandler(CompanyNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleCompanyNotFound(CompanyNotFoundException exception,
			HttpServletRequest request) {

		return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
	}

	@ExceptionHandler(EligibilityCriteriaNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleEligibilityCriteriaNotFound(
			EligibilityCriteriaNotFoundException exception, HttpServletRequest request) {

		return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request);
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ApiErrorResponse> handleIllegalState(IllegalStateException exception,
			HttpServletRequest request) {

		return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException exception,
			HttpServletRequest request) {

		return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleDatabaseConflict(DataIntegrityViolationException exception,
			HttpServletRequest request) {

		return buildResponse(HttpStatus.CONFLICT, "Duplicate or invalid database data", request);
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException exception,
			HttpServletRequest request) {

		HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());

		String error = status != null ? status.getReasonPhrase() : "Request failed";

		String message = exception.getReason();

		if (message == null || message.isBlank()) {

			message = error;
		}

		ApiErrorResponse response = new ApiErrorResponse(LocalDateTime.now(), exception.getStatusCode().value(), error,
				message, request.getRequestURI());

		return ResponseEntity.status(exception.getStatusCode()).headers(exception.getHeaders()).body(response);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(HttpMessageNotReadableException exception,
			HttpServletRequest request) {

		return buildResponse(HttpStatus.BAD_REQUEST,
				"Invalid request body. Check JSON syntax, field types and enum values.", request);
	}

	private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String message,
			HttpServletRequest request) {

		ApiErrorResponse errorResponse = new ApiErrorResponse(LocalDateTime.now(), status.value(),
				status.getReasonPhrase(), message, request.getRequestURI());

		return ResponseEntity.status(status).body(errorResponse);
	}
}