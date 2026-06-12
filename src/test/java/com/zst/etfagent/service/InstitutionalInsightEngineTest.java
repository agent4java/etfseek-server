package com.zst.etfagent.service;

import com.zst.etfagent.domain.EtfFundInfo;
import com.zst.etfagent.domain.IndexInfo;
import com.zst.etfagent.domain.InstitutionalInsight;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InstitutionalInsightEngineTest {

    private final InstitutionalInsightEngine engine = new InstitutionalInsightEngine();

    @Test
    void scoresInstitutionFriendlyEtfsHigher() {
        IndexInfo index = new IndexInfo("000510.SH", "中证A500指数", "SECT_A500", "中证A500", "884002.WI",
                bd("0.95"), bd("0.71"), bd("0.55"), bd("15.9"), bd("1.58"), bd("2.18"), "核心宽基");
        EtfFundInfo strong = fund("563360.SH", "中证A500ETF", "120", "15000", "0.6", "0.02", "0.2");
        EtfFundInfo weak = fund("000000.SH", "低流动性ETF", "5", "200", "2.5", "0.5", "0.8");

        InstitutionalInsight insight = engine.analyze(List.of(index), List.of(weak, strong));

        assertThat(insight.indexFits().get(0).fitLabel()).isEqualTo("High conviction");
        assertThat(insight.etfScorecards().get(0).fundCode()).isEqualTo("563360.SH");
        assertThat(insight.etfScorecards().get(0).strengths()).isNotEmpty();
        assertThat(insight.etfScorecards().get(1).watchItems()).isNotEmpty();
    }

    private static EtfFundInfo fund(String code, String name, String size, String volume, String tracking, String premium, String fee) {
        BigDecimal sizeBillion = bd(size);
        return new EtfFundInfo(code, name, "000510.SH", "中证A500指数", sizeBillion, sizeBillion.multiply(new BigDecimal("100000000")),
                bd("0.15"), bd("0.05"), bd(fee), bd(tracking), bd(premium), bd(volume),
                "基金经理", "SH", "基金公司", "宽基", "2024-01-01");
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
