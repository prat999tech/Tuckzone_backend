package com.school.canteen.exception;

import com.school.canteen.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.validation.FieldError;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.LinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * One place that turns exceptions into consistent JSON error responses, so controllers
 * and services never build error bodies themselves. @RestControllerAdvice makes these
 * handlers apply across every controller.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Any of our expected, typed errors (404, 409, ...). */
    /**
     * Declared ahead of the general ApiException handler and typed more narrowly, so Spring
     * picks it for this subclass. The code in {@code details} is what lets the client tell
     * "verify your email" apart from every other 403 without parsing prose.
     */
    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ApiError> handleEmailNotVerified(EmailNotVerifiedException ex,
                                                          HttpServletRequest request) {
        return build(ex.getStatus(), ex.getMessage(), List.of(), request,
                EmailNotVerifiedException.CODE);
    }

    @ExceptionHandler(FirebaseUserNotRegisteredException.class)
    public ResponseEntity<ApiError> handleFirebaseUserNotRegistered(FirebaseUserNotRegisteredException ex,
                                                          HttpServletRequest request) {
        return build(ex.getStatus(), ex.getMessage(), List.of(), request,
                FirebaseUserNotRegisteredException.CODE);
    }

    @ExceptionHandler(OrderingClosedException.class)
    public ResponseEntity<ApiError> handleOrderingClosed(OrderingClosedException ex,
                                                          HttpServletRequest request) {
        return build(ex.getStatus(), ex.getMessage(), List.of(), request, OrderingClosedException.CODE);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex, HttpServletRequest request) {
        return build(ex.getStatus(), ex.getMessage(), List.of(), request);
    }

    /**
     * A @PreAuthorize denial throws AuthorizationDeniedException (an AccessDeniedException)
     * inside the controller invocation, so it lands here rather than in Security's
     * filter-level handling. Translate it to a clean 403.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex,
                                                       HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Access denied", List.of(), request);
    }

    /** Bean-validation failures on @Valid request bodies -> 400 with per-field messages. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();
        // Also returned keyed by field so a form can highlight the offending input rather
        // than showing the caller a combined blob it would have to parse back apart.
        // Merge keeps the first message when one field trips several constraints — showing
        // one clear reason beats stacking them under a single input.
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() == null
                                ? "Invalid value" : fieldError.getDefaultMessage(),
                        (first, second) -> first,
                        LinkedHashMap::new));
        return build(HttpStatus.BAD_REQUEST, "Validation failed", details, request, null, fieldErrors);
    }

    /**
     * Client-side mistakes that would otherwise fall through to the catch-all and be
     * reported as 500s: an unmapped URL, a wrong HTTP method, a malformed UUID or unknown
     * enum value in the path/query, an unreadable JSON body, or a missing parameter.
     *
     * Returning 5xx for these is actively harmful in production — monitoring treats them
     * as server faults and real outages get lost in the noise.
     */
    @ExceptionHandler({
            NoResourceFoundException.class,
            NoHandlerFoundException.class,
            HttpRequestMethodNotSupportedException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> handleClientErrors(Exception ex, HttpServletRequest request) {
        HttpStatus status = switch (ex) {
            case NoResourceFoundException ignored -> HttpStatus.NOT_FOUND;
            case NoHandlerFoundException ignored -> HttpStatus.NOT_FOUND;
            case HttpRequestMethodNotSupportedException ignored -> HttpStatus.METHOD_NOT_ALLOWED;
            default -> HttpStatus.BAD_REQUEST;
        };
        String message = switch (ex) {
            case NoResourceFoundException ignored -> "Endpoint not found";
            case NoHandlerFoundException ignored -> "Endpoint not found";
            case HttpRequestMethodNotSupportedException ignored ->
                    "HTTP method not supported for this endpoint";
            case MethodArgumentTypeMismatchException mismatch ->
                    "Invalid value for '" + mismatch.getName() + "'";
            case MissingServletRequestParameterException missing ->
                    "Missing required parameter '" + missing.getParameterName() + "'";
            default -> "Malformed request body";
        };
        return build(status, message, List.of(), request);
    }

    /** Anything unexpected -> 500, without leaking internals to the client (but logged). */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong", List.of(), request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message,
                                           List<String> details, HttpServletRequest request) {
        return build(status, message, details, request, null, null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message,
                                           List<String> details, HttpServletRequest request,
                                           String code) {
        return build(status, message, details, request, code, null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message,
                                           List<String> details, HttpServletRequest request,
                                           String code, Map<String, String> fieldErrors) {
        ApiError body = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                details,
                code,
                fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
