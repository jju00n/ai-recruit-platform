package com.example.airecruit.common.dto;

public enum Status {
    SUCCESS(200, "성공"),
    FAIL(-1, "실패"),
    ERROR_OCCURRED(-98, "에러가 발생했습니다."),
    LOGIN_FAIL(90, "아이디 또는 비밀번호가 잘못되었습니다."),
    AUTHENTICATION_FAIL(91, "이메일 인증에 실패했습니다."),
    UNAUTHORIZED(92, "로그인 정보가 유효하지 않습니다."),
    ACCOUNT_LOCK(93, "잠김 계정입니다."),
    ACCESS_DENIED(94, "접근 권한이 없습니다."),
    EXIST_ID(100, "이미 사용중인 이메일입니다."),
    NOT_SAME_PASSWORD(101, "비밀번호가 일치하지 않습니다."),
    INVALID_TOKEN(107, "유효하지 않은 토큰입니다."),
    INVALID_TYPE_VALUE(400, "INVALID_TYPE_VALUE"),
    LOGOUT(201, "로그아웃이 완료되었습니다."),
    COMPANY_NOT_FOUND(110, "존재하지 않는 회사입니다."),
    JOB_POSTING_NOT_FOUND(111, "존재하지 않는 채용공고입니다."),
    RESUME_NOT_FOUND(120, "이력서를 찾을 수 없습니다."),
    APPLICATION_NOT_FOUND(121, "지원 내역을 찾을 수 없습니다."),
    ALREADY_APPLIED(122, "이미 지원한 공고입니다."),
    INVALID_FILE_TYPE(123, "PDF 파일만 업로드 가능합니다."),
    JOB_SERVICE_ERROR(124, "채용공고 정보를 가져올 수 없습니다."),
    AI_ANALYSIS_ERROR(125, "AI 분석 중 오류가 발생했습니다."),
    RESUME_COACHING_ERROR(126, "AI 이력서 코칭 분석에 실패했습니다.");

    private final int code;
    private String message;

    Status(final int code, final String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
