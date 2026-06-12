package com.zst.etfagent.service;

import com.zst.etfagent.domain.AgentRunContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DeterministicResearchWorkflowTest {

    @Autowired
    private DeterministicResearchWorkflow workflow;

    @Test
    void draftsTraceableInstitutionalResearchForTheme() {
        String question = "帮我找半导体相关 ETF，并比较规模、跟踪误差和费率";
        DeterministicResearchWorkflow.ResearchDraft draft = workflow.draft(question, AgentRunContext.defaults());

        assertThat(draft.sectors()).isNotEmpty();
        assertThat(draft.indexes()).isNotEmpty();
        assertThat(draft.candidateEtfs()).isNotEmpty();
        assertThat(draft.dataLineage()).contains("W1055046 searchContent=" + question);
        assertThat(draft.institutionalInsight().indexFits()).isNotEmpty();
        assertThat(draft.institutionalInsight().etfScorecards()).isNotEmpty();
        assertThat(draft.institutionalInsight().portfolioUseCases()).anyMatch(useCase -> useCase.contains("Advisor"));
    }

    @Test
    void draftsDividendLowVolResearchWithRiskNotes() {
        DeterministicResearchWorkflow.ResearchDraft draft = workflow.draft("红利低波主题有哪些 ETF 可以关注", AgentRunContext.defaults());

        assertThat(draft.sectors().get(0).sSectcode()).isEqualTo("SECT_DIVIDEND_LOW_VOL");
        assertThat(draft.riskNotes()).anyMatch(note -> note.contains("do not indicate future returns"));
        assertThat(draft.institutionalInsight().etfScorecards().get(0).bestUseCases()).isNotEmpty();
    }
}
