package util;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public final class TestUtil {

    private static ObjectMapper objectMapper = new ObjectMapper();

    public static APIGatewayProxyRequestEvent createEventRequest(final Object request,
                                                                 final String liquidityPoolName) {
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent();
        String body = null;
        try {
            body = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        requestEvent.setBody(body);
        requestEvent.setPathParameters(getLiquidityPoolPathParam(liquidityPoolName));
        return requestEvent;
    }

    public static APIGatewayProxyRequestEvent createEventRequest(final Object request) {
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent();
        String body = null;
        try {
            body = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        requestEvent.setBody(body);
        return requestEvent;
    }

    public static Map<String, String> getLiquidityPoolPathParam(final String liquidityPoolName) {
        Map<String, String> pathParameters = new HashMap<>();
        pathParameters.put("liquidityPoolName", liquidityPoolName);
        return pathParameters;
    }
}
