package com.serverless.error;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ErrorResponse {
    private String type;
    private String message;

    public static ErrorResponse badRequest(final String message) {
        return new ErrorResponse("BAD_REQUEST", message);
    }
}
