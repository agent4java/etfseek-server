package com.zst.etfagent.api;

public record EtfAgentRequest(
        String question,
        String userId,
        String institutionId,
        String businessScene,
        String complianceMode
) {
}
