package com.zst.etfagent.service;

import com.zst.etfagent.domain.AgentRunContext;
import com.zst.etfagent.domain.EtfFundInfo;
import com.zst.etfagent.domain.IndexInfo;
import com.zst.etfagent.domain.InstitutionalInsight;
import com.zst.etfagent.domain.SectorInfo;
import com.zst.etfagent.seek.CachingEtfSeekService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class DeterministicResearchWorkflow {

    private static final Logger log = LoggerFactory.getLogger(DeterministicResearchWorkflow.class);

    private final CachingEtfSeekService etfSeekService;
    private final InstitutionalInsightEngine insightEngine;

    public DeterministicResearchWorkflow(CachingEtfSeekService etfSeekService, InstitutionalInsightEngine insightEngine) {
        this.etfSeekService = etfSeekService;
        this.insightEngine = insightEngine;
    }

    public ResearchDraft draft(String question, AgentRunContext context) {
        long started = System.currentTimeMillis();
        log.info("[WORKFLOW][draft] start userId={} institutionId={} question={}",
                context.userId(), context.institutionId(), question);
        List<SectorInfo> sectors = etfSeekService.searchSectors(question);
        if (sectors.isEmpty()) {
            log.info("[WORKFLOW][draft] no sectors durationMs={}", System.currentTimeMillis() - started);
            return ResearchDraft.empty(question, "W1055046 did not return related sectors. Try a clearer industry, theme, or index name.");
        }

        SectorInfo sector = sectors.get(0);
        log.info("[WORKFLOW][draft] selected sector code={} name={} dataSource={}",
                sector.sSectcode(), sector.name(), sector.dataSource());
        List<IndexInfo> indexes = etfSeekService.listIndexesBySector(sector.sSectcode());
        if (indexes.isEmpty()) {
            log.info("[WORKFLOW][draft] no indexes sector={} durationMs={}",
                    sector.sSectcode(), System.currentTimeMillis() - started);
            return ResearchDraft.empty(question, "W1055048 did not return related indexes, so ETF mapping cannot continue.");
        }

        IndexInfo index = indexes.get(0);
        log.info("[WORKFLOW][draft] selected index code={} name={} relation={}",
                index.indexCode(), index.indexName(), index.sectorRelation());
        List<EtfFundInfo> etfs = etfSeekService.listEtfsByIndex(index.indexCode()).stream()
                .sorted(Comparator.comparing(EtfFundInfo::fundSizeBillion, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
        InstitutionalInsight insight = insightEngine.analyze(indexes, etfs);
        log.info("[WORKFLOW][draft] insight generated indexes={} etfs={} indexFits={} scorecards={} durationMs={}",
                indexes.size(), etfs.size(), insight.indexFits().size(), insight.etfScorecards().size(),
                System.currentTimeMillis() - started);
        String summary = "ETF SEEK chain completed: theme recognition, index matching, ETF mapping, and institutional due-diligence scoring. "
                + "The result is designed for B2B research, shelf management, and advisor enablement, not direct trading advice.";

        return new ResearchDraft(question, context, summary, sectors, indexes, etfs, insight, List.of(
                "W1055046 searchContent=" + question,
                "W1055048 sSectcode=" + sector.sSectcode(),
                "W1055011 sIrdCode=" + index.indexCode()
        ), List.of(
                "Historical size, fee, tracking error, and premium/discount do not indicate future returns.",
                "ETF selection still requires suitability, liquidity, product documents, and internal compliance review."
        ));
    }

    public record ResearchDraft(
            String question,
            AgentRunContext context,
            String summary,
            List<SectorInfo> sectors,
            List<IndexInfo> indexes,
            List<EtfFundInfo> candidateEtfs,
            InstitutionalInsight institutionalInsight,
            List<String> dataLineage,
            List<String> riskNotes
    ) {

        public static ResearchDraft empty(String question, String warning) {
            return new ResearchDraft(question, AgentRunContext.defaults(), warning, List.of(), List.of(), List.of(),
                    new InstitutionalInsight(List.of(), List.of(), List.of(), List.of(), List.of()),
                    List.of("W1055046 searchContent=" + question), List.of(warning));
        }
    }
}
