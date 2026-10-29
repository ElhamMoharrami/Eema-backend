package com.eema.backend.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private String getPath(WebRequest request) {
        return request.getDescription(false)
                .replace("uri=", "");
    }

    private ErrorResponse buildErrorResponse(
            HttpStatus status,
            String error,
            String message,
            WebRequest request
    ) {
        return new ErrorResponse(
                Instant.now(),
                status.value(),
                error,
                message,
                getPath(request)
        );
    }

    private ValidationErrorResponse buildValidationResponse(
            BindingResult bindingResult,
            WebRequest request
    ) {

        Map<String, String> errors = new LinkedHashMap<>();

        for (FieldError error : bindingResult.getFieldErrors()) {
            errors.putIfAbsent(
                    error.getField(),
                    error.getDefaultMessage()
            );
        }

        return new ValidationErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Error",
                "Validation failed",
                getPath(request),
                errors
        );
    }


    private Map<String, String> extractConstraintErrors(
            ConstraintViolationException ex
    ) {

        Map<String, String> errors = new LinkedHashMap<>();

        ex.getConstraintViolations()
                .forEach(violation -> {

                    String path =
                            violation.getPropertyPath().toString();

                    String field =
                            path.contains(".")
                                    ? path.substring(path.lastIndexOf('.') + 1)
                                    : path;

                    errors.putIfAbsent(
                            field,
                            violation.getMessage()
                    );
                });

        return errors;
    }


    private <T> ResponseEntity<T> response(
            HttpStatus status,
            T body
    ) {
        return ResponseEntity
                .status(status)
                .body(body);
    }


    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            BadRequestException ex,
            WebRequest request
    ) {

        log.warn("Bad request: {}", ex.getMessage());

        return response(
                HttpStatus.BAD_REQUEST,
                buildErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Bad Request",
                        ex.getMessage(),
                        request
                )
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            WebRequest request
    ) {
        log.warn("Invalid request: {}", ex.getMessage());
        return response(HttpStatus.BAD_REQUEST,
                buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request));
    }


    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            UnauthorizedException ex,
            WebRequest request
    ) {

        log.warn("Unauthorized: {}", ex.getMessage());

        return response(
                HttpStatus.UNAUTHORIZED,
                buildErrorResponse(
                        HttpStatus.UNAUTHORIZED,
                        "Unauthorized",
                        ex.getMessage(),
                        request
                )
        );
    }


    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(
            ForbiddenException ex,
            WebRequest request
    ) {

        log.warn("Forbidden: {}", ex.getMessage());

        return response(
                HttpStatus.FORBIDDEN,
                buildErrorResponse(
                        HttpStatus.FORBIDDEN,
                        "Forbidden",
                        ex.getMessage(),
                        request
                )
        );
    }


    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            NotFoundException ex,
            WebRequest request
    ) {

        log.warn("Not found: {}", ex.getMessage());

        return response(
                HttpStatus.NOT_FOUND,
                buildErrorResponse(
                        HttpStatus.NOT_FOUND,
                        "Not Found",
                        ex.getMessage(),
                        request
                )
        );
    }


    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            ConflictException ex,
            WebRequest request
    ) {

        log.warn("Conflict: {}", ex.getMessage());

        return response(
                HttpStatus.CONFLICT,
                buildErrorResponse(
                        HttpStatus.CONFLICT,
                        "Conflict",
                        ex.getMessage(),
                        request
                )
        );
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            WebRequest request
    ) {

        return response(
                HttpStatus.BAD_REQUEST,
                buildValidationResponse(
                        ex.getBindingResult(),
                        request
                )
        );
    }


    @ExceptionHandler(BindException.class)
    public ResponseEntity<ValidationErrorResponse> handleBind(
            BindException ex,
            WebRequest request
    ) {

        return response(
                HttpStatus.BAD_REQUEST,
                buildValidationResponse(
                        ex.getBindingResult(),
                        request
                )
        );
    }


    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            WebRequest request
    ) {

        ValidationErrorResponse response =
                new ValidationErrorResponse(
                        Instant.now(),
                        400,
                        "Validation Error",
                        "Validation failed",
                        getPath(request),
                        extractConstraintErrors(ex)
                );

        return ResponseEntity
                .badRequest()
                .body(response);
    }


    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex,
            WebRequest request
    ) {

        return response(
                HttpStatus.BAD_REQUEST,
                buildErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Bad Request",
                        "Missing parameter: " + ex.getParameterName(),
                        request
                )
        );
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleInvalidJson(
            HttpMessageNotReadableException ex,
            WebRequest request
    ) {

        return response(
                HttpStatus.BAD_REQUEST,
                buildErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Bad Request",
                        "Malformed JSON request",
                        request
                )
        );
    }


    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            WebRequest request
    ) {

        return response(
                HttpStatus.BAD_REQUEST,
                buildErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Bad Request",
                        "Invalid value for parameter: " + ex.getName(),
                        request
                )
        );
    }


    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex,
            WebRequest request
    ) {

        return response(
                HttpStatus.UNAUTHORIZED,
                buildErrorResponse(
                        HttpStatus.UNAUTHORIZED,
                        "Unauthorized",
                        "Authentication failed",
                        request
                )
        );
    }


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            WebRequest request
    ) {

        return response(
                HttpStatus.FORBIDDEN,
                buildErrorResponse(
                        HttpStatus.FORBIDDEN,
                        "Forbidden",
                        "Access denied",
                        request
                )
        );
    }


    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMethod(
            org.springframework.web.HttpRequestMethodNotSupportedException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).headers(ex.getHeaders()).body(
                buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed", "This HTTP method is not supported", request));
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleMissingResource(
            org.springframework.web.servlet.resource.NoResourceFoundException ex, WebRequest request) {
        return response(HttpStatus.NOT_FOUND,
                buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found", "Resource not found", request));
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadTooLarge(Exception ex,WebRequest request) {
        return response(HttpStatus.PAYLOAD_TOO_LARGE,buildErrorResponse(HttpStatus.PAYLOAD_TOO_LARGE,
                "Payload Too Large","Image exceeds the configured maximum size",request));
    }
    @ExceptionHandler(org.springframework.web.multipart.support.MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingUploadPart(Exception ex,WebRequest request) {
        return response(HttpStatus.BAD_REQUEST,buildErrorResponse(HttpStatus.BAD_REQUEST,
                "Bad Request","Missing multipart file",request));
    }
    @ExceptionHandler(org.springframework.web.multipart.MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMalformedMultipart(Exception ex,WebRequest request) {
        return response(HttpStatus.BAD_REQUEST,buildErrorResponse(HttpStatus.BAD_REQUEST,
                "Bad Request","Malformed multipart request",request));
    }
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(Exception ex,WebRequest request) {
        return response(HttpStatus.UNSUPPORTED_MEDIA_TYPE,buildErrorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported Media Type","Unsupported request content type",request));
    }

    @ExceptionHandler(com.eema.backend.service.ai.AiServiceException.class)
    public ResponseEntity<ErrorResponse> handleMatchingProviderFailure(WebRequest request) {
        // Provider causes can contain request credentials, user text or raw response bodies.
        log.warn("Matching provider request failed");
        return response(HttpStatus.BAD_GATEWAY, buildErrorResponse(HttpStatus.BAD_GATEWAY,
                "Bad Gateway", "Venue matching is temporarily unavailable. Please try again.", request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex,
            WebRequest request
    ) {

        log.error(
                "Unexpected application error",
                ex
        );

        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                buildErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Internal Server Error",
                        "An unexpected error occurred",
                        request
                )
        );
    }
}
