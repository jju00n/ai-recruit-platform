package com.example.airecruit.common.exception;

import com.example.airecruit.common.dto.Status;

public class LoginFailException extends BizException {
    public LoginFailException() {
        super(Status.LOGIN_FAIL);
    }
}