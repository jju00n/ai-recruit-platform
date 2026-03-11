package com.example.airecruit.job.exception;

import com.example.airecruit.common.dto.ResultData;
import com.example.airecruit.common.dto.Status;
import com.example.airecruit.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ResultData<?>> handleBizException(BizException e) {
        log.warn("[BizException] code={}, message={}", e.getStatus().getCode(), e.getMessage());
        return ResponseEntity.badRequest().body(ResultData.of(e.getStatus(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResultData<?>> handleValidation(MethodArgumentNotValidException e) {
        log.warn("[Validation] {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ResultData.of(Status.FAIL, e.getBindingResult().getAllErrors().get(0).getDefaultMessage()));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ResultData<?>> handleBindException(BindException e) {
        log.warn("[BindException] {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ResultData.of(Status.FAIL, e.getBindingResult().getAllErrors().get(0).getDefaultMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultData<?>> handleException(Exception e) {
        log.error("[Exception] {}", e.getMessage(), e);
        return ResponseEntity.internalServerError().body(ResultData.of(Status.ERROR_OCCURRED));
    }
}
