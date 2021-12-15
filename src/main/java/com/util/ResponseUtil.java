package com.util;


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.model.error.ErrorResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public final class ResponseUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final int statusCode;
    private final String body;
    private final Map<String, String> headers;

    public String toString() {
        return this.body;
    }

    public static APIGatewayProxyResponseEvent createBadRequest(final String errorMessage, final Context context) {
        final ErrorResponse errorResponse = ErrorResponse.badRequest(errorMessage);
        final String errorResponseBody;
        try {
            errorResponseBody = objectMapper.writeValueAsString(errorResponse);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize error response.", e);
        }
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(400)
                .withBody(errorResponseBody)
                .withHeaders(Collections.singletonMap("RequestId", context.getAwsRequestId()));
    }

    public static APIGatewayProxyResponseEvent createEmptyResponse(final int statusCode, final Context context) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(Collections.singletonMap("RequestId", context.getAwsRequestId()));
    }

    public static APIGatewayProxyResponseEvent createSuccessResponse(final Object object, final Context context) {
        final String objectAsString;
        try {
            objectAsString = objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize error response.", e);
        }
        return new APIGatewayProxyResponseEvent()
                .withBody(objectAsString)
                .withStatusCode(200)
                .withHeaders(Collections.singletonMap("RequestId", context.getAwsRequestId()));
    }
}
