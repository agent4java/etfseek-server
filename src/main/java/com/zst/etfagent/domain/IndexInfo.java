package com.zst.etfagent.domain;

import java.math.BigDecimal;

public record IndexInfo(
        String indexCode,
        String indexName,
        String sectorCode,
        String sectorName,
        String sectorIndexCode,
        BigDecimal sectorRelation,
        BigDecimal sectorCoverWeight,
        BigDecimal sectorCoverRate,
        BigDecimal peTtm,
        BigDecimal pbLf,
        BigDecimal dividendYield,
        String style
) {
}
