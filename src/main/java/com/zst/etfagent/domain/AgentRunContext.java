package com.zst.etfagent.domain;

public record AgentRunContext(
        String userId,
        String institutionId,
        String businessScene,
        String complianceMode
) {

    public static AgentRunContext defaults() {
        return new AgentRunContext("anonymous", "zst-demo", "b2b-research", "strict");
    }
}
