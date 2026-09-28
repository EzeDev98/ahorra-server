package com.ahorra.common.exception;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.UUID;

/**
 * Central exception-to-HTTP-response translation.
 * <p>
 * Most exceptions in this application map 1:1 to an HTTP status with no
 * special handling: no custom message, no logging, no extra
 * {@code ProblemDetail} properties. Those are one-line entries in
 * {@link #SIMPLE_STATUS_MAPPINGS}, resolved by a single handler.
 * <p>
 * Exceptions with real custom behavior — a display title, server-side
 * logging, or extra {@code ProblemDetail} properties — keep their own
 * explicit handler below.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Registry of exceptions that map straight to an HTTP status using
     * {@code ex.getMessage()} as the detail, with no other behavior.
     * Adding a new exception in this category is a one-line entry here —
     * no new handler method needed.
     */
    private static final Map<Class<? extends RuntimeException>, HttpStatus> SIMPLE_STATUS_MAPPINGS = Map.ofEntries(
            Map.entry(UserNotFoundException.class, HttpStatus.NOT_FOUND),
            Map.entry(AuthenticationException.class, HttpStatus.UNAUTHORIZED),
            Map.entry(InternalServerErrorException.class, HttpStatus.INTERNAL_SERVER_ERROR),
            Map.entry(RegistrationException.class, HttpStatus.BAD_REQUEST),
            Map.entry(InternalAuthRegistrationFailedException.class, HttpStatus.INTERNAL_SERVER_ERROR),
            Map.entry(MaxAttemptsExceededException.class, HttpStatus.TOO_MANY_REQUESTS),
            Map.entry(UserAlreadyExistsException.class, HttpStatus.CONFLICT),
            Map.entry(RefreshTokenInvalidException.class, HttpStatus.UNAUTHORIZED),
            Map.entry(OtpResendCooldownException.class, HttpStatus.TOO_MANY_REQUESTS),
            Map.entry(OtpInvalidException.class, HttpStatus.BAD_REQUEST),
            Map.entry(OtpExpiredException.class, HttpStatus.BAD_REQUEST),
            Map.entry(UnauthorizedException.class, HttpStatus.UNAUTHORIZED),
            Map.entry(BadRequestException.class, HttpStatus.BAD_REQUEST),
            Map.entry(EntityNotFoundException.class, HttpStatus.NOT_FOUND)
    );

    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail handleMappedRuntimeException(RuntimeException ex) {
        HttpStatus status = SIMPLE_STATUS_MAPPINGS.get(ex.getClass());
        if (status == null) {
            // Not one of our recognized, simply-mapped exceptions (e.g. an
            // NPE or other unexpected bug) — fall back to exactly the same
            // handling as any other unanticipated error.
            return buildUnexpectedErrorResponse(ex);
        }
        return ProblemDetail.forStatusAndDetail(status, ex.getMessage());
    }

    @ExceptionHandler(InternalAuthCallException.class)
    public ProblemDetail handleInternalAuthCallException(InternalAuthCallException ex) {
        return ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDeniedException(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Access Denied");
        return problem;
    }

    /**
     * Catches any DataAccessException that escapes uncaught from anywhere
     * in the app - connection pool exhaustion, CannotCreateTransactionException
     * when Postgres itself is unreachable, query timeouts, etc. - including
     * the exact window KeepAliveJob#pingDatabase exists to shrink. Without
     * this, that kind of infrastructure failure surfaces as a misleading
     * 500 ("something is broken in your request") instead of a 503 ("the
     * service itself is temporarily down"). Same reasoning as
     * internal-auth-server's identical handler.
     */
    @ExceptionHandler(DataAccessException.class)
    public ProblemDetail handleDataAccessException(DataAccessException ex) {
        log.error("Database connectivity failure", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "The service is temporarily unavailable. Please try again shortly.");
        problem.setTitle("Service Temporarily Unavailable");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        return buildUnexpectedErrorResponse(ex);
    }

    private ProblemDetail buildUnexpectedErrorResponse(Exception ex) {
        String traceId = UUID.randomUUID().toString();
        log.error("[traceId={}] Unhandled exception", traceId, ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
        problem.setProperty("traceId", traceId);
        return problem;
    }
}
