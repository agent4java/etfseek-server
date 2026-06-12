package com.zst.etfagent.domain;

public record SectorInfo(
        String sSectcode,
        String name,
        String dataSource,
        String category,
        String reason
) {
}
