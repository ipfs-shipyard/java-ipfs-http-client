package io.ipfs.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.util.*;

import java.lang.reflect.*;
import java.util.stream.Collectors;

public class JSONParser {
    private static ObjectMapper mapper = new ObjectMapper();
    private static ObjectWriter printer;

    public static Object parse(Object json) {
        if (json == null) {
            return null;
        }
        return parse(json.toString(), HashMap.class);
    }

    public static <T> T parse(String json, Class<T> clazz) {
        if (json == null || "".equals(json.trim())) {
            return null;
        }

        try {
            return mapper.readValue(json, clazz);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static List<?> parseStream(String jsonStream) {
        return Arrays.stream(jsonStream.split("\n"))
                .map(e -> parse(e, HashMap.class))
                .collect(Collectors.toList());
    }

    public static String toString(Object obj) {
        try {
            if (printer == null) {
                printer = mapper.writer();
            }
            return printer.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
