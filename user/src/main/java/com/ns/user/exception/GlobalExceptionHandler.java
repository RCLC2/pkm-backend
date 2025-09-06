package com.ns.user.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * Global exception handler
 * Note 도메인 + CRUD 기준 최소/선택적 예외 처리
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle ServiceException (비즈니스 로직에서 발생)
     */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<CustomExceptionStatus> handleServiceException(ServiceException ex, WebRequest request) {
        log.error("ServiceException: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex.getStatus(), request);
    }

    /**
     * Handle RepositoryException (Repository 계층에서 발생하는 예외)
     */
    @ExceptionHandler(RepositoryException.class)
    public ResponseEntity<CustomExceptionStatus> handleRepositoryException(RepositoryException ex, WebRequest request) {
        log.error("RepositoryException: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex.getStatus(), request);
    }

    /**
     * Handle DtoException (잘못된 DTO 변환/검증 등)
     */
    @ExceptionHandler(DtoException.class)
    public ResponseEntity<CustomExceptionStatus> handleDtoException(DtoException ex, WebRequest request) {
        log.error("DtoException: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex.getStatus(), request);
    }

    /**
     * Handle VoException (도메인 엔티티/VO 관련 검증 실패)
     */
    @ExceptionHandler(VoException.class)
    public ResponseEntity<CustomExceptionStatus> handleEntityException(VoException ex, WebRequest request) {
        log.error("VoException: {}", ex.getMessage(), ex);
        return buildErrorResponse(ex.getStatus(), request);
    }

    /**
     * Handle MethodArgumentNotValidException (DTO @Valid 검증 실패)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CustomExceptionStatus> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex, WebRequest request) {
        log.error("Validation failed: {}", ex.getMessage(), ex);

        // 필드별 오류 메시지를 "field: message" 형태로 결합
        String fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        CustomExceptionStatus errorResponse = new CustomExceptionStatus(
                ExceptionStatus.GENERAL_REQUEST_INVALID_PARAMS,
                fieldErrors
        );

        return ResponseEntity.status(ExceptionStatus.GENERAL_REQUEST_INVALID_PARAMS.getStatusCode())
                .body(errorResponse);
    }

    /**
     * Handle ConstraintViolationException (@PathVariable, @RequestParam 검증 실패)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<CustomExceptionStatus> handleConstraintViolationException(ConstraintViolationException ex, WebRequest request) {
        log.error("ConstraintViolationException: {}", ex.getMessage(), ex);

        String violations = ex.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));

        CustomExceptionStatus errorResponse = new CustomExceptionStatus(
                ExceptionStatus.GENERAL_REQUEST_INVALID_PARAMS,
                violations
        );

        return ResponseEntity.status(ExceptionStatus.GENERAL_REQUEST_INVALID_PARAMS.getStatusCode())
                .body(errorResponse);
    }

    /**
     * Handle MethodArgumentTypeMismatchException (잘못된 Enum 값 등)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<CustomExceptionStatus> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex, WebRequest request) {
        log.error("MethodArgumentTypeMismatchException: {}", ex.getMessage(), ex);

        CustomExceptionStatus errorResponse = new CustomExceptionStatus(
                ExceptionStatus.GENERAL_REQUEST_INVALID_PARAMS,
                ExceptionStatus.GENERAL_REQUEST_INVALID_PARAMS.getMessage()
        );

        return ResponseEntity.status(ExceptionStatus.GENERAL_REQUEST_INVALID_PARAMS.getStatusCode())
                .body(errorResponse);
    }

    /**
     * Handle Generic Exception (그 외 모든 예외)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CustomExceptionStatus> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unhandled Exception: {}", ex.getMessage(), ex);

        CustomExceptionStatus errorResponse = new CustomExceptionStatus(
                ExceptionStatus.GENERAL_INTERNAL_SERVER_ERROR,
                "서버에서 알 수 없는 오류가 발생했습니다."
        );

        return ResponseEntity.status(ExceptionStatus.GENERAL_INTERNAL_SERVER_ERROR.getStatusCode())
                .body(errorResponse);
    }

    /**
     * Build the error response
     */
    private ResponseEntity<CustomExceptionStatus> buildErrorResponse(ExceptionStatus errorCode, WebRequest request) {
        CustomExceptionStatus errorResponse = new CustomExceptionStatus(
                errorCode,
                errorCode.getMessage()
        );
        return ResponseEntity.status(errorCode.getStatusCode()).body(errorResponse);
    }
}
