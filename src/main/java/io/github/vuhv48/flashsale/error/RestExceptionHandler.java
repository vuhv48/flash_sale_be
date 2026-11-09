package io.github.vuhv48.flashsale.error;

import io.github.vuhv48.flashsale.common.api.ApiError;
import io.github.vuhv48.flashsale.common.api.ApiResponse;
import io.github.vuhv48.flashsale.common.api.ErrorStatus;
import io.github.vuhv48.flashsale.common.exception.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class RestExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ApiResponse<Void>> badCredentials(BadCredentialsException ex, HttpServletRequest req) {
		log.warn("[api] Bad credentials uri={} msg={}", req.getRequestURI(), ex.getMessage());
		return respond(ErrorStatus.AUTH_FAILED, ex.getMessage(), req);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiResponse<Void>> authentication(AuthenticationException ex, HttpServletRequest req) {
		log.warn("[api] Unauthorized uri={} type={} msg={}", req.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage());
		return respond(ErrorStatus.UNAUTHORIZED, ex.getMessage(), req);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiResponse<Void>> accessDenied(AccessDeniedException ex, HttpServletRequest req) {
		log.warn("[api] Forbidden uri={} msg={}", req.getRequestURI(), ex.getMessage());
		return respond(ErrorStatus.FORBIDDEN, ErrorStatus.FORBIDDEN.defaultMessage(), req);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
		String details = ex.getBindingResult().getFieldErrors().stream()
				.map(RestExceptionHandler::formatFieldError)
				.collect(Collectors.joining("; "));
		log.warn("[api] Validation uri={} — {}", req.getRequestURI(), details);
		var error = ApiError.of(ErrorStatus.VALIDATION_ERROR.httpStatus(), ErrorStatus.VALIDATION_ERROR.code(), details, req.getRequestURI());
		return ResponseEntity.status(ErrorStatus.VALIDATION_ERROR.httpStatus()).body(ApiResponse.fail(error));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<Void>> constraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
		String details = ex.getConstraintViolations().stream()
				.map(v -> v.getPropertyPath() + ": " + v.getMessage())
				.collect(Collectors.joining("; "));
		log.warn("[api] Constraint violation uri={} — {}", req.getRequestURI(), details);
		var error = ApiError.of(ErrorStatus.CONSTRAINT_VIOLATION.httpStatus(), ErrorStatus.CONSTRAINT_VIOLATION.code(), details, req.getRequestURI());
		return ResponseEntity.status(ErrorStatus.CONSTRAINT_VIOLATION.httpStatus()).body(ApiResponse.fail(error));
	}

	@ExceptionHandler(DomainException.class)
	public ResponseEntity<ApiResponse<Void>> domain(DomainException ex, HttpServletRequest req) {
		log.warn("[api] Domain uri={} code={} msg={}", req.getRequestURI(), ex.getCode(), ex.getMessage());
		return ErrorStatus.resolve(ex.getCode())
				.map(status -> respond(status, ex.getMessage(), req))
				.orElseGet(() -> respond(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage(), req));
	}

	@ExceptionHandler(RedisSystemException.class)
	public ResponseEntity<ApiResponse<Void>> redisSystem(RedisSystemException ex, HttpServletRequest req) {
		String msg = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
		log.error("[api] Redis uri={}", req.getRequestURI(), ex);
		return respond(ErrorStatus.DATA_ACCESS_ERROR,
				msg != null && !msg.isBlank()
						? ("Không kết nối được Redis: " + msg)
						: ErrorStatus.DATA_ACCESS_ERROR.defaultMessage(),
				req);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Void>> httpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
		log.warn("[api] Invalid request body uri={}", req.getRequestURI(), ex);
		return respond(ErrorStatus.VALIDATION_ERROR, "Body JSON không hợp lệ", req);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<Void>> illegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
		log.warn("[api] Illegal argument uri={} msg={}", req.getRequestURI(), ex.getMessage());
		return respond(ErrorStatus.INVALID_ARGUMENT, ex.getMessage(), req);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiResponse<Void>> dataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
		log.warn("[api] Data integrity uri={}", req.getRequestURI(), ex);
		return respond(ErrorStatus.DATA_INTEGRITY, ErrorStatus.DATA_INTEGRITY.defaultMessage(), req);
	}

	@ExceptionHandler(DataAccessException.class)
	public ResponseEntity<ApiResponse<Void>> dataAccess(DataAccessException ex, HttpServletRequest req) {
		log.error("[api] Data access uri={}", req.getRequestURI(), ex);
		return respond(ErrorStatus.DATA_ACCESS_ERROR, ErrorStatus.DATA_ACCESS_ERROR.defaultMessage(), req);
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ApiResponse<Void>> illegalState(IllegalStateException ex, HttpServletRequest req) {
		log.error("[api] Illegal state uri={}", req.getRequestURI(), ex);
		return respond(ErrorStatus.ILLEGAL_STATE, ex.getMessage(), req);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> fallback(Exception ex, HttpServletRequest req) {
		log.error("[api] Unhandled exception uri={}", req.getRequestURI(), ex);
		return respond(ErrorStatus.INTERNAL_ERROR, ErrorStatus.INTERNAL_ERROR.defaultMessage(), req);
	}

	private static String formatFieldError(FieldError fe) {
		return fe.getField() + ": " + fe.getDefaultMessage();
	}

	private static ResponseEntity<ApiResponse<Void>> respond(ErrorStatus status, String message, HttpServletRequest req) {
		String msg = (message != null && !message.isBlank()) ? message : status.defaultMessage();
		var error = ApiError.of(status.httpStatus(), status.code(), msg, req.getRequestURI());
		return ResponseEntity.status(status.httpStatus()).body(ApiResponse.fail(error));
	}

	private static ResponseEntity<ApiResponse<Void>> respond(HttpStatus status, String code, String message, HttpServletRequest req) {
		var error = ApiError.of(status.value(), code, message, req.getRequestURI());
		return ResponseEntity.status(status).body(ApiResponse.fail(error));
	}
}
