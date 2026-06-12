package com.zst.etfagent.compliance;

import com.agent4j.api.OutputGuardrail;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EtfComplianceGuardrailsTest {

    private final EtfComplianceGuardrails guardrails = new EtfComplianceGuardrails();

    @Test
    void appendsDisclaimerToNormalResearchOutput() {
        OutputGuardrail.OutputGuardrailResult result = guardrails.outputGuardrail()
                .process("这里是 ETF 对比研究摘要。", null);

        assertThat(result.getOutput().toString()).contains("不构成投资建议");
    }

    @Test
    void rewritesPromissoryOutput() {
        OutputGuardrail.OutputGuardrailResult result = guardrails.outputGuardrail()
                .process("这只 ETF 明天一定涨，可以买入。", null);

        assertThat(result.getOutput().toString()).contains("研究比较");
        assertThat(result.getOutput().toString()).contains("不构成投资建议");
    }
}
