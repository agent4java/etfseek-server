package com.zst.etfagent.domain;

import java.math.BigDecimal;

public record EtfFundInfo(
        String fundCode,
        String fundName,
        String indexCode,
        String indexName,
        BigDecimal fundSizeBillion,
        BigDecimal netAsset,
        BigDecimal managementFeeRate,
        BigDecimal custodyFeeRate,
        BigDecimal totalFeeRate,
        BigDecimal trackingError,
        BigDecimal iopvPremiumDiscount,
        BigDecimal volumeMa20,
        String manager,
        String listingMarket,
        String managementCompany,
        String fundType,
        String setupDate
) {
}
