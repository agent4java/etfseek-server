package com.zst.etfagent.audit;

import com.zst.etfagent.config.EtfAgentProperties;
import com.zst.etfagent.domain.AgentRunContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final EtfAgentProperties properties;

    public AuditLogService(EtfAgentProperties properties) {
        this.properties = properties;
    }

    public void recordStarted(String question, AgentRunContext context) {
        if (properties.isAuditEnabled()) {
            log.info("etf_agent_audit_started time={} userId={} institutionId={} scene={} question={}",
                    Instant.now(), context.userId(), context.institutionId(), context.businessScene(), question);
        }
    }

    public void recordCompleted(String question, AgentRunContext context, Object finalOutput) {
        if (properties.isAuditEnabled()) {
            log.info("etf_agent_audit_completed time={} userId={} institutionId={} scene={} question={} outputLength={}",
                    Instant.now(), context.userId(), context.institutionId(), context.businessScene(), question,
                    finalOutput == null ? 0 : finalOutput.toString().length());
        }
    }
}
