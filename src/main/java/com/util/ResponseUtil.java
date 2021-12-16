package com.util;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
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

    private final int statusCode;
    private final String body;
    private final Map<String, String> headers;

    /**
     * AwsRequestId header.
     */
    private static final String REQUEST_ID = "RequestId";

    public String toString() {
        return this.body;
    }

    public static APIGatewayProxyResponseEvent createBadRequest(final String errorMessage, final Context context) {
        final ErrorResponse errorResponse = ErrorResponse.badRequest(errorMessage);
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(400)
                .withBody(ObjectMapperUtil.toString(errorResponse))
                .withHeaders(Collections.singletonMap(REQUEST_ID, context.getAwsRequestId()));
    }

    public static APIGatewayProxyResponseEvent createEmptyResponse(final int statusCode, final Context context) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(Collections.singletonMap(REQUEST_ID, context.getAwsRequestId()));
    }

    public static APIGatewayProxyResponseEvent createSuccessResponse(final Object object, final Context context) {
        return new APIGatewayProxyResponseEvent()
                .withBody(ObjectMapperUtil.toString(object))
                .withStatusCode(200)
                .withHeaders(Collections.singletonMap(REQUEST_ID, context.getAwsRequestId()));
    }
}
