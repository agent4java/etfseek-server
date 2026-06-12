package com.zst.etfagent.domain;

import java.math.BigDecimal;
import java.util.List;

public record IndexDetailInfo(
        String indexCode,
        String indexName,
        BigDecimal peTtm,
        BigDecimal pbLf,
        BigDecimal dividendYield,
        List<IndexTrendPoint> indexTrend,
        List<PeriodReturn> periodReturns,
        List<Holding> topHoldings,
        List<String> linkedEtfCodes
) {

    public record IndexTrendPoint(String date, BigDecimal close) {
    }

    public record PeriodReturn(String period, BigDecimal returnRate) {
    }

    public record Holding(String stockCode, String stockName, BigDecimal weight) {
    }
}
