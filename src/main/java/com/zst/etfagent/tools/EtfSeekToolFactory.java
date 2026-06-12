package com.zst.etfagent.tools;

import com.agent4j.api.Tool;
import com.zst.etfagent.domain.ToolEnvelope;
import com.zst.etfagent.seek.CachingEtfSeekService;
import com.zst.etfagent.service.DeterministicResearchWorkflow;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class EtfSeekToolFactory {

    private final CachingEtfSeekService etfSeekService;
    private final DeterministicResearchWorkflow researchWorkflow;

    public EtfSeekToolFactory(CachingEtfSeekService etfSeekService, DeterministicResearchWorkflow researchWorkflow) {
        this.etfSeekService = etfSeekService;
        this.researchWorkflow = researchWorkflow;
    }

    public List<Tool> allTools() {
        return List.of(
                searchSectorsTool(),
                listIndexesBySectorTool(),
                listEtfsByIndexTool(),
                getEtfDetailTool(),
                getIndexDetailTool(),
                generateInstitutionalInsightTool()
        );
    }

    public Tool searchSectorsTool() {
        return new JsonTool(
                "w1055046_search_sectors",
                "Search ETF SEEK concept sectors by investment theme or keyword. Maps to W1055046 with request field searchContent.",
                JsonTool.requiredStringSchema("keyword", "Investment theme or keyword, e.g. 半导体, 红利低波, 港股科技"),
                args -> {
                    String keyword = value(args, "keyword");
                    return ToolEnvelope.of(
                            etfSeekService.searchSectors(keyword),
                            "W1055046",
                            Map.of("searchContent", keyword)
                    );
                }
        );
    }

    public Tool listIndexesBySectorTool() {
        return new JsonTool(
                "w1055048_list_indexes_by_sector",
                "List related indexes by ETF SEEK sector code. Maps to W1055048 with sSectcode and returns sectRelation, sectCoverWeight, and sectCoverRate.",
                JsonTool.requiredStringSchema("sSectcode", "Sector code returned by W1055046"),
                args -> {
                    String sectorCode = value(args, "sSectcode");
                    return ToolEnvelope.of(
                            etfSeekService.listIndexesBySector(sectorCode),
                            "W1055048",
                            Map.of("sSectcode", sectorCode)
                    );
                }
        );
    }

    public Tool listEtfsByIndexTool() {
        return new JsonTool(
                "w1055011_list_etfs_by_index",
                "List ETFs linked to an index. Maps to W1055011 with sIrdCode and returns fund scale, manager, company, fee, tracking error, IOPV premium/discount, and 20-day turnover.",
                JsonTool.requiredStringSchema("indexCode", "Index code, request field sIrdCode"),
                args -> {
                    String indexCode = value(args, "indexCode");
                    return ToolEnvelope.of(
                            etfSeekService.listEtfsByIndex(indexCode),
                            "W1055011",
                            Map.of("sIrdCode", indexCode)
                    );
                }
        );
    }

    public Tool getEtfDetailTool() {
        return new JsonTool(
                "get_etf_detail",
                "Get ETF fund detail by fund code. Combines W1055002 fund detail, W1055003 NAV trend, and W1055004 fund/index period return.",
                JsonTool.requiredStringSchema("fundCode", "ETF fund code, request field sIrdCode"),
                args -> {
                    String fundCode = value(args, "fundCode");
                    return ToolEnvelope.of(
                            etfSeekService.getEtfDetail(fundCode),
                            "W1055002+W1055003+W1055004",
                            Map.of("sIrdCode", fundCode)
                    );
                }
        );
    }

    public Tool getIndexDetailTool() {
        return new JsonTool(
                "get_index_detail",
                "Get index detail and linked ETF codes. Use when the user asks about valuation, index trend, period return, holdings, or linked ETFs.",
                JsonTool.requiredStringSchema("indexCode", "Index code"),
                args -> {
                    String indexCode = value(args, "indexCode");
                    return ToolEnvelope.of(
                            etfSeekService.getIndexDetail(indexCode),
                            "INDEX_DETAIL",
                            Map.of("indexCode", indexCode)
                    );
                }
        );
    }

    public Tool generateInstitutionalInsightTool() {
        return new JsonTool(
                "generate_institutional_insight",
                "Run the full ETF SEEK research workflow and generate institutional due-diligence insight: data lineage, index fit score, ETF scorecard, risk flags, and B2B use cases.",
                JsonTool.requiredStringSchema("question", "User research question or theme"),
                args -> {
                    String question = value(args, "question");
                    return ToolEnvelope.of(
                            researchWorkflow.draft(question, com.zst.etfagent.domain.AgentRunContext.defaults()),
                            "W1055046+W1055048+W1055011+INSIGHT_ENGINE",
                            Map.of("question", question)
                    );
                }
        );
    }

    private static String value(Map<String, Object> args, String key) {
        Object value = args.get(key);
        return value == null ? "" : value.toString();
    }
}
