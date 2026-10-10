package com.shesuma.shesumamart.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

public final class JsonUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtil() {}

    public static String toJson(Object value) throws IOException {
        return MAPPER.writeValueAsString(value);
    }
}

