package com.solidariaplus.identity.identityaccess.infrastructure.adapter.in.rest;

import com.solidariaplus.identity.identityaccess.domain.exception.DomainException;
import com.solidariaplus.identity.identityaccess.domain.exception.ErrorCode;
import com.solidariaplus.identity.identityaccess.infrastructure.config.CorrelationId;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = ProblemDetailsAdvice.class)
class ProblemDetailsAdvice {
    private static final Map<ErrorCode, HttpStatus> STATUS = new EnumMap<>(Map.of(
            ErrorCode.UNAUTHENTICATED, HttpStatus.UNAUTHORIZED,
            ErrorCode.AUTH_ACCOUNT_LOCKED, HttpStatus.LOCKED,
            ErrorCode.AUTH_ACTIVATION_CODE_INVALID, HttpStatus.BAD_REQUEST,
            ErrorCode.AUTH_MFA_REQUIRED, HttpStatus.UNAUTHORIZED,
            ErrorCode.MEMBER_NOT_FOUND, HttpStatus.NOT_FOUND,
            ErrorCode.MEMBER_NOT_ELIGIBLE, HttpStatus.UNPROCESSABLE_CONTENT,
            ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND,
            ErrorCode.SAME_PERSON_NOT_ALLOWED, HttpStatus.UNPROCESSABLE_CONTENT,
            ErrorCode.VALIDATION_FAILED, HttpStatus.UNPROCESSABLE_CONTENT));

    @ExceptionHandler(DomainException.class)
    ProblemDetail domain(DomainException exception, HttpServletRequest request) {
        return problem(STATUS.get(exception.getCode()), exception.getCode(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, request);
        List<Map<String, String>> errors = exception.getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "reason", String.valueOf(error.getDefaultMessage())))
                .toList();
        problem.setProperty("errors", errors);
        return problem;
    }

    private static ProblemDetail problem(HttpStatus status, ErrorCode code, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setType(URI.create("https://errors.solidariaplus.ec/" + code.name()));
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code.name());
        problem.setProperty("correlationId", CorrelationId.current());
        return problem;
    }
}
