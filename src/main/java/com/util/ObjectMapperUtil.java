package com.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.model.exception.InvalidInputException;
import lombok.extern.slf4j.Slf4j;

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

    public static String toString(final Object input) throws InvalidInputException {
        try {
            return objectMapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize,", e);
            throw new InvalidInputException(e);
        }
    }
}