package com.zst.etfagent.domain;

import java.math.BigDecimal;
import java.util.List;

public record InstitutionalInsight(
        List<IndexFit> indexFits,
        List<EtfScorecard> etfScorecards,
        List<String> portfolioUseCases,
        List<String> researchAngles,
        List<String> complianceNotes
) {

    public record IndexFit(
            String indexCode,
            String indexName,
            BigDecimal fitScore,
            String fitLabel,
            List<String> evidence
    ) {
    }

    public record EtfScorecard(
            String fundCode,
            String fundName,
            BigDecimal institutionScore,
            String institutionLabel,
            List<String> strengths,
            List<String> watchItems,
            List<String> bestUseCases
    ) {
    }
}
