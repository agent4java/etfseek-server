package com.zst.etfagent.seek;

import com.zst.etfagent.domain.EtfDetailInfo;
import com.zst.etfagent.domain.EtfFundInfo;
import com.zst.etfagent.domain.IndexDetailInfo;
import com.zst.etfagent.domain.IndexInfo;
import com.zst.etfagent.domain.SectorInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "zst.etf-agent", name = "mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockEtfSeekClient implements EtfSeekClient {

    @Override
    public List<SectorInfo> searchSectors(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim();
        if (normalized.contains("红利")) {
            return List.of(new SectorInfo("SECT_DIVIDEND_LOW_VOL", "红利低波", "WD", "Smart Beta", "关键词命中红利、低波、股息相关主题"));
        }
        if (normalized.contains("港股") || normalized.contains("科技")) {
            return List.of(new SectorInfo("SECT_HK_TECH", "港股科技", "WD", "跨境科技", "关键词命中港股科技和互联网平台相关主题"));
        }
        if (normalized.contains("A500") || normalized.contains("中证A500")) {
            return List.of(new SectorInfo("SECT_A500", "中证A500", "WD", "宽基", "关键词命中核心宽基指数主题"));
        }
        return List.of(new SectorInfo("SECT_SEMICONDUCTOR", "半导体", "WD", "行业主题", "关键词命中芯片、半导体、人工智能算力链相关主题"));
    }

    @Override
    public List<IndexInfo> listIndexesBySector(String sectorCode) {
        return switch (sectorCode) {
            case "SECT_DIVIDEND_LOW_VOL" -> List.of(
                    index("000922.SH", "中证红利指数", sectorCode, "红利低波", "884000.WI", "0.92", "0.32", "0.21", "7.80", "0.83", "5.42", "高股息"),
                    index("931446.CSI", "红利低波100指数", sectorCode, "红利低波", "884000.WI", "0.88", "0.28", "0.19", "8.15", "0.91", "4.88", "红利低波")
            );
            case "SECT_HK_TECH" -> List.of(
                    index("HSTECH.HI", "恒生科技指数", sectorCode, "港股科技", "884001.WI", "0.90", "0.42", "0.25", "25.30", "2.61", "0.72", "成长科技")
            );
            case "SECT_A500" -> List.of(
                    index("000510.SH", "中证A500指数", sectorCode, "中证A500", "884002.WI", "0.95", "0.71", "0.55", "15.90", "1.58", "2.18", "核心宽基")
            );
            default -> List.of(
                    index("931865.CSI", "中证半导体材料设备主题指数", sectorCode, "半导体", "884003.WI", "0.91", "0.46", "0.31", "42.60", "4.95", "0.31", "高弹性成长"),
                    index("990001.CSI", "中华交易服务半导体芯片指数", sectorCode, "半导体", "884003.WI", "0.86", "0.39", "0.25", "38.20", "4.22", "0.45", "芯片主题")
            );
        };
    }

    @Override
    public List<EtfFundInfo> listEtfsByIndex(String indexCode) {
        return switch (indexCode) {
            case "000922.SH" -> List.of(
                    etf("510880.SH", "红利ETF", indexCode, "中证红利指数", "182.40", "0.50", "0.10", "0.60", "1.12", "-0.08", "9800", "基金经理A", "华泰柏瑞基金", "大盘", "2006-11-17"),
                    etf("515180.SH", "红利低波ETF", indexCode, "中证红利指数", "46.70", "0.50", "0.10", "0.60", "1.35", "0.03", "4200", "基金经理B", "易方达基金", "Smart Beta", "2019-11-26")
            );
            case "000510.SH" -> List.of(
                    etf("563360.SH", "中证A500ETF", indexCode, "中证A500指数", "96.10", "0.15", "0.05", "0.20", "0.72", "0.05", "12300", "基金经理C", "招商基金", "宽基", "2024-09-10"),
                    etf("159338.SZ", "A500ETF指数基金", indexCode, "中证A500指数", "88.50", "0.15", "0.05", "0.20", "0.81", "-0.02", "11600", "基金经理D", "国泰基金", "宽基", "2024-09-12")
            );
            case "HSTECH.HI" -> List.of(
                    etf("513180.SH", "恒生科技指数ETF", indexCode, "恒生科技指数", "211.30", "0.50", "0.10", "0.60", "1.60", "0.12", "18800", "基金经理E", "华夏基金", "跨境", "2021-05-18"),
                    etf("513130.SH", "恒生科技ETF", indexCode, "恒生科技指数", "73.20", "0.50", "0.10", "0.60", "1.77", "-0.04", "9600", "基金经理F", "易方达基金", "跨境", "2021-05-19")
            );
            default -> List.of(
                    etf("512480.SH", "半导体ETF", indexCode, "半导体主题指数", "258.90", "0.50", "0.10", "0.60", "1.84", "0.06", "21600", "基金经理G", "国联安基金", "行业主题", "2019-05-08"),
                    etf("159995.SZ", "芯片ETF", indexCode, "半导体主题指数", "221.60", "0.50", "0.10", "0.60", "1.91", "-0.03", "20100", "基金经理H", "华夏基金", "行业主题", "2020-01-20")
            );
        };
    }

    @Override
    public EtfDetailInfo getEtfDetail(String fundCode) {
        return new EtfDetailInfo(
                fundCode,
                fundCode + " ETF样例详情",
                List.of(
                        new EtfDetailInfo.PerformancePoint("2026-01-31", bd("1.0000")),
                        new EtfDetailInfo.PerformancePoint("2026-03-31", bd("1.0420")),
                        new EtfDetailInfo.PerformancePoint("2026-05-31", bd("1.0180"))
                ),
                List.of(
                        new EtfDetailInfo.PeriodReturn("本日", bd("-2.86")),
                        new EtfDetailInfo.PeriodReturn("本月", bd("-6.84")),
                        new EtfDetailInfo.PeriodReturn("本年", bd("-4.36")),
                        new EtfDetailInfo.PeriodReturn("近一年", bd("30.22")),
                        new EtfDetailInfo.PeriodReturn("近三年", bd("30.86"))
                ),
                bd("1.56"),
                bd("0.04"),
                bd("0.50"),
                bd("0.10"),
                bd("10000"),
                "基金经理",
                "示例基金公司",
                "2020-01-01",
                List.of("主题/行业 ETF 波动通常高于宽基 ETF", "历史表现不代表未来收益")
        );
    }

    @Override
    public IndexDetailInfo getIndexDetail(String indexCode) {
        return new IndexDetailInfo(
                indexCode,
                indexCode + " 指数样例详情",
                bd("32.40"),
                bd("3.28"),
                bd("1.12"),
                List.of(
                        new IndexDetailInfo.IndexTrendPoint("2026-01-31", bd("1000.00")),
                        new IndexDetailInfo.IndexTrendPoint("2026-03-31", bd("1065.00")),
                        new IndexDetailInfo.IndexTrendPoint("2026-05-31", bd("1028.00"))
                ),
                List.of(
                        new IndexDetailInfo.PeriodReturn("近1月", bd("1.92")),
                        new IndexDetailInfo.PeriodReturn("近3月", bd("-2.18")),
                        new IndexDetailInfo.PeriodReturn("近1年", bd("11.35"))
                ),
                List.of(
                        new IndexDetailInfo.Holding("688981", "中芯国际", bd("8.62")),
                        new IndexDetailInfo.Holding("603501", "韦尔股份", bd("6.15")),
                        new IndexDetailInfo.Holding("002371", "北方华创", bd("5.94"))
                ),
                List.of("512480.SH", "159995.SZ")
        );
    }

    private static IndexInfo index(
            String code,
            String name,
            String sectorCode,
            String sectorName,
            String sectorIndexCode,
            String relation,
            String coverWeight,
            String coverRate,
            String pe,
            String pb,
            String dividend,
            String style
    ) {
        return new IndexInfo(code, name, sectorCode, sectorName, sectorIndexCode, bd(relation), bd(coverWeight), bd(coverRate), bd(pe), bd(pb), bd(dividend), style);
    }

    private static EtfFundInfo etf(
            String code,
            String name,
            String indexCode,
            String indexName,
            String sizeBillion,
            String managementFee,
            String custodyFee,
            String totalFee,
            String trackingError,
            String premiumDiscount,
            String volumeMa20,
            String manager,
            String company,
            String fundType,
            String setupDate
    ) {
        String market = code.endsWith(".SZ") ? "SZ" : "SH";
        BigDecimal size = bd(sizeBillion);
        return new EtfFundInfo(code, name, indexCode, indexName, size, size.multiply(new BigDecimal("100000000")),
                bd(managementFee), bd(custodyFee), bd(totalFee), bd(trackingError), bd(premiumDiscount), bd(volumeMa20),
                manager, market, company, fundType, setupDate);
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
