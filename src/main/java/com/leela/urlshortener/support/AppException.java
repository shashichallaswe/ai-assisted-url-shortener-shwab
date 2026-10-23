package com.leela.urlshortener.support;

import java.util.List;

public class AppException extends RuntimeException {

    private final ErrorCode code;
    private final List<FieldError> details;

    public AppException(ErrorCode code, String message) {
        this(code, message, List.of());
    }

    public AppException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.details = List.of();
    }

    public AppException(ErrorCode code, String message, List<FieldError> details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public ErrorCode code() {
        return code;
    }

    public List<FieldError> details() {
        return details;
    }

    public record FieldError(String field, String issue) {}
}
