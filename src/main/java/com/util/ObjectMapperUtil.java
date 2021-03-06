package com.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.model.exception.InvalidInputException;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility ObjectMapper class to utilize one static reusable instance of object mapper to serialize and deserialize
 * requests. Also handles error handling and translates any errors into our internal InvalidInputException.
 */
@Slf4j
public final class ObjectMapperUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T> T toClass(final String input, final Class<T> clazz) throws InvalidInputException {
        try {
            return objectMapper.readValue(input, clazz);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize,", e);
            throw new InvalidInputException(e);
        }
    }

    /**
     * Attempts to serialize a given object. Should succeed, but if not, throws a runtime exception which is bubbled up.
     */
    public static String toString(final Object input) {
        try {
            return objectMapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize,", e);
            throw new RuntimeException(e);
        }
    }
}
