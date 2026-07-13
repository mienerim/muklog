package com.muklog.backend.common;

import com.muklog.backend.auth.InvalidGoogleTokenException;
import com.muklog.backend.record.RecordNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecordNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(RecordNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(InvalidGoogleTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidGoogleToken(InvalidGoogleTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("validation failed"));
    }

    // e.g. a Google token missing the email claim would violate users.email NOT NULL
    // on first-time signup. Surface a clean 409 with a log line instead of a raw 500.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("data integrity violation"));
    }
}
