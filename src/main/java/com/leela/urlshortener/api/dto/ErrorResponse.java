package com.leela.urlshortener.api.dto;

import com.leela.urlshortener.support.AppException;
import java.util.List;

public record ErrorResponse(ErrorBody error) {

    public record ErrorBody(String code, String message, List<AppException.FieldError> details) {}

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new ErrorBody(code, message, List.of()));
    }

    public static ErrorResponse of(String code, String message, List<AppException.FieldError> details) {
        return new ErrorResponse(new ErrorBody(code, message, details));
    }
}
