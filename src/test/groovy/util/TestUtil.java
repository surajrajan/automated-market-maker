package util;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.util.ObjectMapperUtil;

import java.util.HashMap;
import java.util.Map;

public final class TestUtil {

    public static APIGatewayProxyRequestEvent createEventRequest(final Object request,
                                                                 final String poolName) {
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent();
        requestEvent.setBody(ObjectMapperUtil.toString(request));
        requestEvent.setPathParameters(getPoolNamePathParams(poolName));
        return requestEvent;
    }

    public static APIGatewayProxyRequestEvent createEventRequest(final Object request) {
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent();
        requestEvent.setBody(ObjectMapperUtil.toString(request));
        return requestEvent;
    }

    public static Map<String, String> getPoolNamePathParams(final String poolName) {
        Map<String, String> pathParameters = new HashMap<>();
        pathParameters.put("poolName", poolName);
        return pathParameters;
    }
}
