package com.zst.etfagent.api;

public record EtfAgentResponse(
        String question,
        Object finalOutput,
        String lastAgent,
        int turns
) {
}
