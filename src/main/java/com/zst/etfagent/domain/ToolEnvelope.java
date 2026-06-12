package com.zst.etfagent.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ToolEnvelope<T>(
        T data,
        String sourceApi,
        Map<String, Object> inputParams,
        LocalDate asOfDate,
        List<String> warnings
) {

    public static <T> ToolEnvelope<T> of(T data, String sourceApi, Map<String, Object> inputParams) {
        return new ToolEnvelope<>(data, sourceApi, inputParams, LocalDate.now(), List.of());
    }

    public static <T> ToolEnvelope<T> withWarnings(
            T data,
            String sourceApi,
            Map<String, Object> inputParams,
            List<String> warnings
    ) {
        return new ToolEnvelope<>(data, sourceApi, inputParams, LocalDate.now(), warnings);
    }
}
