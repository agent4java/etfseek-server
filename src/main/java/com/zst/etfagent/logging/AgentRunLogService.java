package com.zst.etfagent.logging;

import com.agent4j.api.RunConfig;
import com.agent4j.api.RunEvent;
import com.agent4j.api.RunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AgentRunLogService {

    private static final Logger log = LoggerFactory.getLogger(AgentRunLogService.class);

    public RunConfig runConfig(String channel) {
        return RunConfig.builder()
                .eventConsumer(event -> logEvent(channel, event))
                .build();
    }

    private void logEvent(String channel, RunEvent event) {
        String agentName = event.getAgent() == null ? "-" : event.getAgent().getName();
        switch (event.getType()) {
            case RUN_STARTED -> log.info("[AGENT][{}] run started turn={} agent={} input={}",
                    channel, event.getTurn(), agentName, abbreviate(event.getPayload(), 240));
            case AGENT_STARTED -> log.info("[AGENT][{}] agent started turn={} agent={}",
                    channel, event.getTurn(), agentName);
            case MODEL_STARTED -> log.info("[AGENT][{}] model started turn={} agent={} toolSpecs={}",
                    channel, event.getTurn(), agentName, countToolSpecs(event.getPayload()));
            case MODEL_COMPLETED -> log.info("[AGENT][{}] model completed turn={} agent={} payload={}",
                    channel, event.getTurn(), agentName, abbreviate(event.getPayload(), 180));
            case MODEL_FAILED -> log.warn("[AGENT][{}] model failed turn={} agent={} error={}",
                    channel, event.getTurn(), agentName, errorMessage(event.getPayload()));
            case MODEL_DELTA -> log.debug("[AGENT][{}] model delta turn={} agent={} delta={}",
                    channel, event.getTurn(), agentName, abbreviate(event.getPayload(), 120));
            case TOOL_STARTED -> log.info("[AGENT][{}] tool started turn={} agent={} tool={} args={}",
                    channel, event.getTurn(), agentName, event.getName(), abbreviate(event.getPayload(), 300));
            case TOOL_COMPLETED -> log.info("[AGENT][{}] tool completed turn={} agent={} tool={} result={}",
                    channel, event.getTurn(), agentName, event.getName(), abbreviate(event.getPayload(), 300));
            case HANDOFF -> log.info("[AGENT][{}] handoff turn={} from={} to={}",
                    channel, event.getTurn(), agentName, event.getName());
            case GUARDRAIL -> log.info("[AGENT][{}] guardrail turn={} agent={} name={} result={}",
                    channel, event.getTurn(), agentName, event.getName(), abbreviate(event.getPayload(), 180));
            case RUN_COMPLETED -> log.info("[AGENT][{}] run completed turn={} agent={} result={}",
                    channel, event.getTurn(), agentName, summarizeRunResult(event.getPayload()));
            case RUN_FAILED -> log.warn("[AGENT][{}] run failed turn={} agent={} error={}",
                    channel, event.getTurn(), agentName, errorMessage(event.getPayload()));
            default -> log.info("[AGENT][{}] event={} turn={} agent={} name={} payload={}",
                    channel, event.getType(), event.getTurn(), agentName, event.getName(), abbreviate(event.getPayload(), 180));
        }
    }

    private static String summarizeRunResult(Object payload) {
        if (payload instanceof RunResult result) {
            String lastAgent = result.getLastAgent() == null ? "-" : result.getLastAgent().getName();
            return "lastAgent=" + lastAgent
                    + ", turns=" + result.getCurrentTurn() + "/" + result.getMaxTurns()
                    + ", finalOutput=" + abbreviate(result.getFinalOutput(), 240);
        }
        return abbreviate(payload, 240);
    }

    private static int countToolSpecs(Object payload) {
        if (payload instanceof com.agent4j.model.ModelInvocationRequest request) {
            return request.getToolSpecs().size();
        }
        return -1;
    }

    private static String errorMessage(Object payload) {
        if (payload instanceof Throwable throwable) {
            return throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
        }
        return abbreviate(payload, 240);
    }

    private static String abbreviate(Object value, int maxLength) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).replaceAll("\\s+", " ").trim();
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
