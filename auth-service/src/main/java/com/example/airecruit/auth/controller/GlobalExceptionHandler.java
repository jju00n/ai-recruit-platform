package com.example.airecruit.auth.controller;

import com.example.airecruit.common.dto.ResultData;
import com.example.airecruit.common.dto.Status;
import com.example.airecruit.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ResultData<?>> handleBizException(BizException e) {
        log.warn("BizException: {}", e.getMessage());
        return ResponseEntity.badRequest().body(ResultData.of(e.getStatus()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResultData<?>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("IllegalArgumentException: {}", e.getMessage());
        return ResponseEntity.badRequest().body(ResultData.of(Status.FAIL, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultData<?>> handleException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity.internalServerError().body(ResultData.of(Status.ERROR_OCCURRED));
    }
}
