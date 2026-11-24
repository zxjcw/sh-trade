package com.shiguangji.common.exception;

import com.shiguangji.common.api.ErrorCodes;
import com.shiguangji.common.api.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBiz(BizException ex) {
        HttpStatus status = ex.getCode() == ErrorCodes.UNAUTHORIZED ? HttpStatus.UNAUTHORIZED : HttpStatus.OK;
        return ResponseEntity.status(status).body(Result.fail(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleInvalid(MethodArgumentNotValidException ex) {
        FieldError error = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = error == null ? "参数不合法" : error.getDefaultMessage();
        return ResponseEntity.ok(Result.fail(ErrorCodes.BAD_REQUEST, message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraint(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream().findFirst()
                .map(v -> v.getMessage()).orElse("参数不合法");
        return ResponseEntity.ok(Result.fail(ErrorCodes.BAD_REQUEST, message));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<Result<Void>> handleBadBody(Exception ex) {
        return ResponseEntity.ok(Result.fail(ErrorCodes.BAD_REQUEST, "请求参数格式不正确"));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> handleDuplicate(DuplicateKeyException ex) {
        log.warn("duplicate key: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.ok(Result.fail(ErrorCodes.CONFLICT, "操作冲突，请稍后重试"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOther(Exception ex) {
        log.error("unhandled error", ex);
        return ResponseEntity.ok(Result.fail(ErrorCodes.INTERNAL, "服务器开小差了，请稍后重试"));
    }
}
