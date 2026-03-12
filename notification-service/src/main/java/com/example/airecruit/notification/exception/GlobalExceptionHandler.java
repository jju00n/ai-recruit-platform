package com.example.airecruit.notification.exception;

import com.example.airecruit.common.dto.ResultData;
import com.example.airecruit.common.dto.Status;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultData<?>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.internalServerError().body(ResultData.of(Status.ERROR_OCCURRED));
    }
}
