package com.example.demo.transit.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleStatus(ResponseStatusException ex, jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("error", Messages.translate(ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString(), request.getHeader("Accept-Language"))));
    }

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<Map<String, String>> handleBadCredentials(Exception ex, jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", Messages.translate("البريد الإلكتروني أو كلمة السر غير صحيحة", request.getHeader("Accept-Language"))));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex, jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", Messages.translate("ليس لديك الصلاحيات الكافية لتنفيذ هذا الإجراء", request.getHeader("Accept-Language"))));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex, jakarta.servlet.http.HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .orElse(Messages.translate("طلب غير صالح", request.getHeader("Accept-Language")));
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(Exception ex, jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.badRequest().body(Map.of("error", Messages.translate("طلب غير صالح: تحقق من شكل البيانات المرسلة", request.getHeader("Accept-Language"))));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(DuplicateKeyException ex, jakarta.servlet.http.HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", Messages.translate("تعذر تنفيذ العملية: السجل موجود مسبقاً (قيمة مكررة)", request.getHeader("Accept-Language"))));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex, jakarta.servlet.http.HttpServletRequest request) {
        log.error("Unhandled error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", Messages.translate("حدث خطأ غير متوقع في السيرفر", request.getHeader("Accept-Language"))));
    }
}
