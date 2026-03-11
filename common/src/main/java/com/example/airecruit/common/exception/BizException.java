package com.example.airecruit.common.exception;

import com.example.airecruit.common.dto.Status;

public class BizException extends RuntimeException {
    private final Status status;

    public BizException(Status status) {
        super(status.getMessage());
        this.status = status;
    }

    public BizException(Status status, String message) {
        super(message);
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }
}