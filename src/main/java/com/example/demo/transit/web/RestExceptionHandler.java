package com.example.demo.transit.web;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {

    // 1. معالجة الأخطاء المرمية عبر ResponseStatusException
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("error", ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString()));
    }

    // 2. معالجة أخطاء كلمة السر أو البريد غير الصحيح أثناء تسجيل الدخول
    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<Map<String, String>> handleBadCredentials(Exception ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "اسم المستخدم أو كلمة السر غير صحيحة"));
    }

    // 3. معالجة محاولة الوصول لمسار غير مصرح به (مثلاً زبون عادي يحاول إرسال طلب أدمن)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "ليس لديك الصلاحيات الكافية لتنفيذ هذا الإجراء"));
    }

    // 4. معالجة أخطاء التحقق من صحة المدخلات Valid@
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + " " + err.getDefaultMessage())
                .orElse("طلب غير صالح");
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    // 5. معالجة الترويسات المفقودة
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, String>> handleHeader(MissingRequestHeaderException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "إرسال البلاغ يتطلب توثيقاً عبر الترويسة " + ex.getHeaderName()));
    }

    // 6. معالجة إرسال أنواع بيانات خاطئة في الرابط (مثل إرسال نص بدلاً من رقم الـ ID)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "نوع المعلمة " + ex.getName() + " غير صالح"));
    }

    // 7. معالجة قيود قاعدة البيانات (مثل تكرار إيميل مخصص كـ Unique)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "تعذر تنفيذ العملية بسبب تعارض في البيانات (ربما السجل موجود مسبقاً)"));
    }

    // 8. معالجة أي خطأ غبر متوقع في السيرفر (Catch-All)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "حدث خطأ غير متوقع في السيرفر"));
    }
}