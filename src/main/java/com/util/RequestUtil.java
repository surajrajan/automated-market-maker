package com.util;

import com.config.ErrorMessages;
import com.model.exception.InvalidInputException;

import java.util.Map;

/**
 * Utility class for service request handling.
 */
public final class RequestUtil {

    /**
     * Extracts the liquidity pool name from the path parameters map. Also ensures the name is valid.
     *
     * @param pathParameters
     * @throws InvalidInputException when not found or invalid asset name
     */
    public static String extractPoolNameFromPathParams(final Map<String, String> pathParameters, final String pathParameter)
            throws InvalidInputException {
        if (pathParameters == null) {
            throw new InvalidInputException(ErrorMessages.INVALID_REQUEST_MISSING_FIELDS);
        }
        String pathParameterValue = pathParameters.get(pathParameter);
        if (pathParameterValue == null || pathParameterValue.isEmpty()) {
            throw new InvalidInputException(ErrorMessages.INVALID_REQUEST_MISSING_FIELDS);
        }
        return pathParameterValue;
    }
}
