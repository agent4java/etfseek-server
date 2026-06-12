package com.zst.etfagent.service;

import com.zst.etfagent.domain.EtfFundInfo;
import com.zst.etfagent.domain.IndexInfo;
import com.zst.etfagent.domain.InstitutionalInsight;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class InstitutionalInsightEngine {

    public InstitutionalInsight analyze(List<IndexInfo> indexes, List<EtfFundInfo> funds) {
        List<InstitutionalInsight.IndexFit> indexFits = indexes.stream()
                .map(this::scoreIndex)
                .sorted(Comparator.comparing(InstitutionalInsight.IndexFit::fitScore).reversed())
                .toList();

        List<InstitutionalInsight.EtfScorecard> scorecards = funds.stream()
                .map(this::scoreFund)
                .sorted(Comparator.comparing(InstitutionalInsight.EtfScorecard::institutionScore).reversed())
                .toList();

        return new InstitutionalInsight(
                indexFits,
                scorecards,
                List.of(
                        "Advisor enablement: explain why a client theme maps to a concrete index and ETF shelf.",
                        "Research triage: rank candidates by scale, liquidity, fee, tracking quality, and premium/discount.",
                        "Product shelf governance: identify low-liquidity, high-premium, or high-tracking-error watchlist products.",
                        "Morning meeting draft: produce traceable theme-index-ETF evidence for internal distribution."
                ),
                List.of(
                        "Index popularity alone is insufficient; theme relevance and constituent coverage should be shown.",
                        "Large ETF scale reduces operational friction, but should not be treated as a return signal.",
                        "Tracking error, IOPV premium/discount, and 20-day average turnover support execution-risk discussion.",
                        "Fee advantage is more meaningful for long holding-period narratives than short-term trading narratives."
                ),
                List.of(
                        "Scores are for B2B research ordering only and are not investment advice.",
                        "Institutional rollout should combine product risk level, investor suitability, internal whitelist, and sales-region rules."
                )
        );
    }

    private InstitutionalInsight.IndexFit scoreIndex(IndexInfo index) {
        BigDecimal score = cap(value(index.sectorRelation()).multiply(new BigDecimal("55"))
                .add(value(index.sectorCoverWeight()).multiply(new BigDecimal("2")).multiply(new BigDecimal("25")))
                .add(value(index.sectorCoverRate()).multiply(new BigDecimal("2")).multiply(new BigDecimal("20"))));
        List<String> evidence = new ArrayList<>();
        evidence.add("sectRelation=" + printable(index.sectorRelation()) + " shows concept-index relevance.");
        evidence.add("sectCoverWeight=" + printable(index.sectorCoverWeight()) + " shows concept exposure weight.");
        evidence.add("sectCoverRate=" + printable(index.sectorCoverRate()) + " shows constituent-pool concept purity.");
        if (index.peTtm() != null || index.pbLf() != null || index.dividendYield() != null) {
            evidence.add("valuation PE/PB/dividend=" + printable(index.peTtm()) + "/" + printable(index.pbLf()) + "/" + printable(index.dividendYield()));
        }
        return new InstitutionalInsight.IndexFit(index.indexCode(), index.indexName(), score, label(score), evidence);
    }

    private InstitutionalInsight.EtfScorecard scoreFund(EtfFundInfo fund) {
        BigDecimal scaleScore = normalize(fund.fundSizeBillion(), new BigDecimal("0"), new BigDecimal("200"));
        BigDecimal liquidityScore = normalize(fund.volumeMa20(), new BigDecimal("0"), new BigDecimal("20000"));
        BigDecimal trackingScore = inverseNormalize(fund.trackingError(), new BigDecimal("0"), new BigDecimal("3"));
        BigDecimal feeScore = inverseNormalize(firstNonNull(fund.totalFeeRate(), fund.managementFeeRate()), new BigDecimal("0"), new BigDecimal("1"));
        BigDecimal premiumScore = inverseNormalize(abs(fund.iopvPremiumDiscount()), new BigDecimal("0"), new BigDecimal("1"));
        BigDecimal score = cap(scaleScore.multiply(new BigDecimal("25"))
                .add(liquidityScore.multiply(new BigDecimal("25")))
                .add(trackingScore.multiply(new BigDecimal("25")))
                .add(feeScore.multiply(new BigDecimal("15")))
                .add(premiumScore.multiply(new BigDecimal("10"))));

        List<String> strengths = new ArrayList<>();
        List<String> watchItems = new ArrayList<>();
        if (gte(fund.fundSizeBillion(), "50")) {
            strengths.add("Scale is institution-friendly for initial shelf screening.");
        } else {
            watchItems.add("Scale is limited; review creation/redemption and block-trade capacity.");
        }
        if (gte(fund.volumeMa20(), "5000")) {
            strengths.add("20-day average turnover supports execution convenience.");
        } else {
            watchItems.add("20-day average turnover is low; highlight secondary-market liquidity risk.");
        }
        if (lte(fund.trackingError(), "1")) {
            strengths.add("Tracking error is low, useful for replication-quality narratives.");
        } else {
            watchItems.add("Tracking error needs explanation; do not compare only by short-term return.");
        }
        if (lte(abs(fund.iopvPremiumDiscount()), "0.10")) {
            strengths.add("IOPV premium/discount is moderate.");
        } else {
            watchItems.add("IOPV premium/discount is elevated; warn about price-NAV deviation.");
        }

        return new InstitutionalInsight.EtfScorecard(
                fund.fundCode(),
                fund.fundName(),
                score,
                label(score),
                strengths,
                watchItems,
                useCases(fund)
        );
    }

    private static List<String> useCases(EtfFundInfo fund) {
        List<String> cases = new ArrayList<>();
        if (gte(fund.fundSizeBillion(), "100")) {
            cases.add("Core candidate for same-theme ETF comparison.");
        }
        if (gte(fund.volumeMa20(), "10000")) {
            cases.add("Execution candidate for intraday trading or switching discussion.");
        }
        if (lte(firstNonNull(fund.totalFeeRate(), fund.managementFeeRate()), "0.30")) {
            cases.add("Low-fee candidate for long holding-period narratives.");
        }
        if (cases.isEmpty()) {
            cases.add("Watchlist candidate pending more liquidity and performance evidence.");
        }
        return cases;
    }

    private static BigDecimal normalize(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value == null || max.compareTo(min) == 0) {
            return BigDecimal.ZERO;
        }
        return ratioCap(value.subtract(min).divide(max.subtract(min), 6, RoundingMode.HALF_UP));
    }

    private static BigDecimal inverseNormalize(BigDecimal value, BigDecimal min, BigDecimal max) {
        return BigDecimal.ONE.subtract(normalize(value, min, max)).max(BigDecimal.ZERO);
    }

    private static BigDecimal ratioCap(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }
        return value;
    }

    private static BigDecimal cap(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal upper = new BigDecimal("100");
        BigDecimal capped = value.compareTo(upper) > 0 ? upper : value;
        return capped.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal abs(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.abs();
    }

    private static BigDecimal firstNonNull(BigDecimal first, BigDecimal second) {
        return first != null ? first : second;
    }

    private static boolean gte(BigDecimal value, String threshold) {
        return value != null && value.compareTo(new BigDecimal(threshold)) >= 0;
    }

    private static boolean lte(BigDecimal value, String threshold) {
        return value != null && value.compareTo(new BigDecimal(threshold)) <= 0;
    }

    private static String label(BigDecimal score) {
        if (score.compareTo(new BigDecimal("80")) >= 0) {
            return "High conviction";
        }
        if (score.compareTo(new BigDecimal("60")) >= 0) {
            return "Research candidate";
        }
        return "Watchlist";
    }

    private static String printable(BigDecimal value) {
        return value == null ? "N/A" : value.stripTrailingZeros().toPlainString();
    }
}
