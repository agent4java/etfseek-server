package com.zst.etfagent.domain;

import java.math.BigDecimal;
import java.util.List;

public record EtfDetailInfo(
        String fundCode,
        String fundName,
        List<PerformancePoint> navTrend,
        List<PeriodReturn> periodReturns,
        BigDecimal trackingError,
        BigDecimal iopvPremiumDiscount,
        BigDecimal managementFeeRate,
        BigDecimal custodyFeeRate,
        BigDecimal volumeMa20,
        String manager,
        String managementCompany,
        String setupDate,
        List<String> riskNotes
) {

    public record PerformancePoint(String date, BigDecimal nav) {
    }

    public record PeriodReturn(String period, BigDecimal returnRate) {
    }
}
