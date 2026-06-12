package com.zst.etfagent.tools;

import com.agent4j.api.Tool;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

public class JsonTool implements Tool {

    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    private final String name;
    private final String description;
    private final Map<String, Object> parameterSchema;
    private final Function<Map<String, Object>, Object> function;

    public JsonTool(
            String name,
            String description,
            Map<String, Object> parameterSchema,
            Function<Map<String, Object>, Object> function
    ) {
        this.name = name;
        this.description = description;
        this.parameterSchema = parameterSchema;
        this.function = function;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        return parameterSchema;
    }

    @Override
    public Object invoke(ToolContext context) {
        Object result = function.apply(parse(context.getArgumentsJson()));
        try {
            return MAPPER.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize tool result: " + name, e);
        }
    }

    private static Map<String, Object> parse(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = MAPPER.readValue(json, Map.class);
            return parsed == null ? Collections.emptyMap() : parsed;
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    public static Map<String, Object> requiredStringSchema(String propertyName, String description) {
        return Map.of(
                "type", "object",
                "properties", Map.of(propertyName, Map.of(
                        "type", "string",
                        "description", description
                )),
                "required", java.util.List.of(propertyName)
        );
    }
}
