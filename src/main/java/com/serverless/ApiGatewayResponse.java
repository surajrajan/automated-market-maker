package com.serverless;


import com.amazonaws.services.lambda.runtime.Context;
import com.api.model.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class ApiGatewayResponse {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final int statusCode;
    private final String body;
    private final Map<String, String> headers;

    public String toString() {
        return this.body;
    }

    public static ApiGatewayResponse createBadRequest(final String errorMessage, final Context context) {
        ErrorResponse errorResponse = ErrorResponse.badRequest(errorMessage);
        String errorResponseBody;
        try {
            errorResponseBody = objectMapper.writeValueAsString(errorResponse);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize error response.", e);
        }
        return ApiGatewayResponse.builder()
                .statusCode(400)
                .body(errorResponseBody)
                .headers(Collections.singletonMap("RequestId", context.getAwsRequestId()))
                .build();
    }

    public static ApiGatewayResponse createEmptyResponse(final int statusCode, final Context context) {
        return ApiGatewayResponse.builder()
                .statusCode(statusCode)
                .headers(Collections.singletonMap("RequestId", context.getAwsRequestId()))
                .build();
    }

    public static ApiGatewayResponse createSuccessResponse(final Object object, final Context context) {
        String objectAsString;
        try {
            objectAsString = objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize error response.", e);
        }
        return ApiGatewayResponse.builder()
                .body(objectAsString)
                .statusCode(200)
                .headers(Collections.singletonMap("RequestId", context.getAwsRequestId()))
                .build();
    }
}
