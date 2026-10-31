package com.nadav.dlp.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.net.URI;

/** Translates business, validation and HTTP errors into consistent Problem Detail responses. */
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handle(ApiException ex, HttpServletRequest request) {
        return problem(ex.status(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var result = problem(HttpStatus.BAD_REQUEST, "Request validation failed", request);
        result.getBody().setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage()).sorted().toList());
        return result;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ProblemDetail> malformed(Exception ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Malformed request: check JSON fields, types and UUIDs", request);
    }

    /** Converts database relationship races to 409 without exposing SQL or internal constraint names. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> conflict(DataIntegrityViolationException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Operation conflicts with existing resource relationships", request);
    }

    @ExceptionHandler({HandlerMethodValidationException.class, HttpRequestMethodNotSupportedException.class,
        HttpMediaTypeNotSupportedException.class, NoResourceFoundException.class})
    ResponseEntity<ProblemDetail> requestError(Exception ex, HttpServletRequest request) {
        var error = (ErrorResponse) ex;
        var status = HttpStatus.valueOf(error.getStatusCode().value());
        var result = problem(status, status.getReasonPhrase(), request);
        return ResponseEntity.status(status).headers(error.getHeaders()).body(result.getBody());
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail, HttpServletRequest request) {
        var body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setInstance(URI.create(request.getRequestURI()));
        return ResponseEntity.status(status).body(body);
    }
}
