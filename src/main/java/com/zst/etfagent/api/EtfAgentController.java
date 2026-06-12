package com.zst.etfagent.api;

import com.agent4j.api.Agent;
import com.agent4j.api.AgentRunner;
import com.agent4j.api.RunRequest;
import com.agent4j.api.RunResult;
import com.agent4j.memory.InMemorySession;
import com.agent4j.sse.AgentSseService;
import com.zst.etfagent.audit.AuditLogService;
import com.zst.etfagent.domain.AgentRunContext;
import com.zst.etfagent.logging.AgentRunLogService;
import com.zst.etfagent.service.DeterministicResearchWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/etf-agent")
public class EtfAgentController {

    private static final Logger log = LoggerFactory.getLogger(EtfAgentController.class);

    private final AgentRunner agentRunner;
    private final AgentSseService agentSseService;
    private final Agent etfResearchAgent;
    private final AuditLogService auditLogService;
    private final DeterministicResearchWorkflow deterministicWorkflow;
    private final AgentRunLogService agentRunLogService;

    public EtfAgentController(
            AgentRunner agentRunner,
            AgentSseService agentSseService,
            Agent etfResearchAgent,
            AuditLogService auditLogService,
            DeterministicResearchWorkflow deterministicWorkflow,
            AgentRunLogService agentRunLogService
    ) {
        this.agentRunner = agentRunner;
        this.agentSseService = agentSseService;
        this.etfResearchAgent = etfResearchAgent;
        this.auditLogService = auditLogService;
        this.deterministicWorkflow = deterministicWorkflow;
        this.agentRunLogService = agentRunLogService;
    }

    @PostMapping("/chat")
    public EtfAgentResponse chat(@RequestBody EtfAgentRequest request) {
        AgentRunContext context = contextOf(request);
        log.info("[HTTP][chat] received userId={} institutionId={} scene={} question={}",
                context.userId(), context.institutionId(), context.businessScene(), request.question());
        auditLogService.recordStarted(request.question(), context);
        RunResult result = agentRunner.run(
                etfResearchAgent,
                RunRequest.builder()
                        .input(request.question())
                        .context(context)
                        .session(new InMemorySession(context.userId()))
                        .maxTurns(12)
                        .build()
                ,
                agentRunLogService.runConfig("chat")
        );
        auditLogService.recordCompleted(request.question(), context, result.getFinalOutput());
        log.info("[HTTP][chat] completed userId={} lastAgent={} turns={}",
                context.userId(),
                result.getLastAgent() == null ? "-" : result.getLastAgent().getName(),
                result.getCurrentTurn());
        return new EtfAgentResponse(
                request.question(),
                result.getFinalOutput(),
                result.getLastAgent() == null ? null : result.getLastAgent().getName(),
                result.getCurrentTurn()
        );
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @RequestParam String q,
            @RequestParam(defaultValue = "anonymous") String userId,
            @RequestParam(defaultValue = "zst-demo") String institutionId,
            @RequestParam(defaultValue = "b2b-research") String businessScene
    ) {
        AgentRunContext context = new AgentRunContext(userId, institutionId, businessScene, "strict");
        log.info("[HTTP][stream] received userId={} institutionId={} scene={} question={}",
                context.userId(), context.institutionId(), context.businessScene(), q);
        auditLogService.recordStarted(q, context);
        return agentSseService.stream(
                etfResearchAgent,
                RunRequest.builder()
                        .input(q)
                        .context(context)
                        .session(new InMemorySession(userId))
                        .maxTurns(12)
                        .build()
                ,
                agentRunLogService.runConfig("stream")
        );
    }

    @PostMapping("/research/draft")
    public DeterministicResearchWorkflow.ResearchDraft draft(@RequestBody EtfAgentRequest request) {
        AgentRunContext context = contextOf(request);
        log.info("[HTTP][draft] received userId={} institutionId={} scene={} question={}",
                context.userId(), context.institutionId(), context.businessScene(), request.question());
        auditLogService.recordStarted(request.question(), context);
        DeterministicResearchWorkflow.ResearchDraft draft = deterministicWorkflow.draft(request.question(), context);
        auditLogService.recordCompleted(request.question(), context, draft.summary());
        log.info("[HTTP][draft] completed sectors={} indexes={} etfs={} scorecards={}",
                draft.sectors().size(),
                draft.indexes().size(),
                draft.candidateEtfs().size(),
                draft.institutionalInsight().etfScorecards().size());
        return draft;
    }

    private static AgentRunContext contextOf(EtfAgentRequest request) {
        return new AgentRunContext(
                blankToDefault(request.userId(), "anonymous"),
                blankToDefault(request.institutionId(), "zst-demo"),
                blankToDefault(request.businessScene(), "b2b-research"),
                blankToDefault(request.complianceMode(), "strict")
        );
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
