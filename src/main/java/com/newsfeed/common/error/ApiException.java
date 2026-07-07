package com.newsfeed.common.error;

import org.springframework.http.HttpStatus;

/**
 * API 오류를 표현하는 공통 예외. code는 클라이언트가 분기할 수 있는 기계용 식별자다.
 * (docs/04-api-spec.md 오류 응답 형식)
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    private ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static ApiException unauthorized(String code, String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, code, message);
    }

    public static ApiException notFound(String code, String message) {
        return new ApiException(HttpStatus.NOT_FOUND, code, message);
    }

    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message);
    }

    public static ApiException tooManyRequests(String code, String message) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, code, message);
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }
}
