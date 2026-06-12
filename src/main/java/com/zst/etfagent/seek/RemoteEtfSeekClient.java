package com.zst.etfagent.seek;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.zst.etfagent.config.EtfAgentProperties;
import com.zst.etfagent.domain.EtfDetailInfo;
import com.zst.etfagent.domain.EtfFundInfo;
import com.zst.etfagent.domain.IndexDetailInfo;
import com.zst.etfagent.domain.IndexInfo;
import com.zst.etfagent.domain.SectorInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "zst.etf-agent", name = "mock-enabled", havingValue = "false")
public class RemoteEtfSeekClient implements EtfSeekClient {

    private static final Logger log = LoggerFactory.getLogger(RemoteEtfSeekClient.class);

    private final RestTemplate restTemplate;
    private final EtfAgentProperties properties;
    private final RemoteAuthorizationProvider authorizationProvider;

    public RemoteEtfSeekClient(
            RestTemplateBuilder restTemplateBuilder,
            EtfAgentProperties properties,
            RemoteAuthorizationProvider authorizationProvider
    ) {
        this.restTemplate = restTemplateBuilder.build();
        this.properties = properties;
        this.authorizationProvider = authorizationProvider;
    }

    @Override
    public List<SectorInfo> searchSectors(String keyword) {
        JsonNode data = post("W1055046", Map.of(
                "searchContent", keyword,
                "dataSource", properties.getDataSource()
        )).path("data");
        List<SectorInfo> sectors = new ArrayList<>();
        for (JsonNode item : iterable(data)) {
            sectors.add(new SectorInfo(
                    text(item, "sSectcode"),
                    text(item, "sSectname"),
                    text(item, "dataSource"),
                    "concept",
                    "Matched by W1055046 fuzzy concept search"
            ));
        }
        return sectors;
    }

    @Override
    public List<IndexInfo> listIndexesBySector(String sectorCode) {
        JsonNode records = post("W1055048", Map.of(
                "sSectcode", sectorCode,
                "currentPage", 1,
                "pageSize", 20,
                "orderField", "sectRelation",
                "orderFlag", 0
        )).path("data").path("records");
        List<IndexInfo> indexes = new ArrayList<>();
        for (JsonNode item : iterable(records)) {
            indexes.add(new IndexInfo(
                    text(item, "sIrdcode"),
                    text(item, "sIrdname"),
                    text(item, "sSectcode"),
                    text(item, "sSectname"),
                    text(item, "sSectIrdcode"),
                    decimal(item, "sectRelation"),
                    decimal(item, "sectCoverWeight"),
                    decimal(item, "sectCoverRate"),
                    null,
                    null,
                    null,
                    "ETF SEEK concept index"
            ));
        }
        return indexes;
    }

    @Override
    public List<EtfFundInfo> listEtfsByIndex(String indexCode) {
        JsonNode response = post("W1055011", Map.of("sIrdCode", indexCode));
        JsonNode data = response.has("data") ? response.path("data") : response;
        List<EtfFundInfo> funds = new ArrayList<>();
        for (JsonNode item : iterable(data)) {
            funds.add(toFundInfo(item));
        }
        return funds;
    }

    @Override
    public EtfDetailInfo getEtfDetail(String fundCode) {
        JsonNode detail = post("W1055002", Map.of("sIrdCode", fundCode)).path("data");
        JsonNode nav = post("W1055003", Map.of("sIrdCode", fundCode)).path("data");
        JsonNode returns = post("W1055004", Map.of("sIrdCode", fundCode)).path("data");

        List<EtfDetailInfo.PerformancePoint> trend = new ArrayList<>();
        for (JsonNode item : iterable(nav)) {
            trend.add(new EtfDetailInfo.PerformancePoint(text(item, "date"), decimal(item, "navAdj")));
        }

        List<EtfDetailInfo.PeriodReturn> periodReturns = new ArrayList<>();
        JsonNode fundReturns = returns.path("aidx");
        addReturn(periodReturns, "day", fundReturns, "avgReturnDay");
        addReturn(periodReturns, "week-to-date", fundReturns, "avgReturnWtd");
        addReturn(periodReturns, "quarter-to-date", fundReturns, "avgReturnQtd");
        addReturn(periodReturns, "month-to-date", fundReturns, "avgReturnMtd");
        addReturn(periodReturns, "year-to-date", fundReturns, "avgReturnYtd");
        addReturn(periodReturns, "1-year", fundReturns, "avgReturnYear");
        addReturn(periodReturns, "3-year", fundReturns, "avgReturn3Year");

        return new EtfDetailInfo(
                text(detail, "sIrdCode"),
                text(detail, "abbrName"),
                trend,
                periodReturns,
                decimal(detail, "trackingError"),
                decimal(detail, "iopvDiscountRate"),
                decimal(detail, "manageFeeRatio"),
                decimal(detail, "custFeeRatio"),
                decimal(detail, "volumeMa20"),
                text(detail, "managerName"),
                text(detail, "managementComp"),
                text(detail, "setUpDt"),
                List.of("ETF detail assembled from W1055002, W1055003 and W1055004")
        );
    }

    @Override
    public IndexDetailInfo getIndexDetail(String indexCode) {
        List<EtfFundInfo> linkedEtfs = listEtfsByIndex(indexCode);
        return new IndexDetailInfo(
                indexCode,
                indexCode,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                linkedEtfs.stream().map(EtfFundInfo::fundCode).toList()
        );
    }

    private EtfFundInfo toFundInfo(JsonNode item) {
        BigDecimal netAsset = decimal(item, "netAsset");
        BigDecimal sizeBillion = netAsset == null ? null : netAsset.divide(new BigDecimal("100000000"), 4, RoundingMode.HALF_UP);
        BigDecimal totalFee = firstNonNull(decimal(item, "manageCustFeeRatio"), decimal(item, "feeRatio"));
        return new EtfFundInfo(
                text(item, "sIrdCode"),
                text(item, "abbrName"),
                text(item, "sIndexIrdCode"),
                text(item, "sIrdName"),
                sizeBillion,
                netAsset,
                decimal(item, "manageFeeRatio"),
                decimal(item, "custFeeRatio"),
                totalFee,
                decimal(item, "trackingError"),
                decimal(item, "iopvDiscountRate"),
                decimal(item, "volumeMa20"),
                text(item, "managerName"),
                marketOf(text(item, "sIrdCode")),
                text(item, "managementComp"),
                text(item, "idxType2"),
                text(item, "setUpDt")
        );
    }

    private JsonNode post(String api, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        authorizationProvider.currentAuthorization().ifPresent(value -> headers.set("authorization", value));
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        String url = properties.getBaseUrl() + "/" + api;
        long started = System.currentTimeMillis();
        boolean hasAuthorization = authorizationProvider.currentAuthorization().isPresent();
        log.info("[REMOTE][POST] api={} url={} bodyKeys={} authorizationPresent={}",
                api, url, body.keySet(), hasAuthorization);
        JsonNode response = restTemplate.postForObject(url, entity, JsonNode.class);
        log.info("[REMOTE][POST-DONE] api={} durationMs={} response={}",
                api, System.currentTimeMillis() - started, summarizeResponse(response));
        return response == null ? JsonNodeFactory.instance.objectNode() : response;
    }

    private static String summarizeResponse(JsonNode response) {
        if (response == null) {
            return "null";
        }
        JsonNode success = response.path("success");
        JsonNode status = response.path("status");
        JsonNode msg = response.path("msg");
        JsonNode data = response.path("data");
        String dataSummary;
        if (data.isArray()) {
            dataSummary = "array(size=" + data.size() + ")";
        } else if (data.isObject()) {
            JsonNode records = data.path("records");
            dataSummary = records.isArray() ? "object(records=" + records.size() + ")" : "object";
        } else {
            dataSummary = data.isMissingNode() ? "missing" : data.getNodeType().name();
        }
        return "success=" + (success.isMissingNode() ? "N/A" : success.asText())
                + ", status=" + (status.isMissingNode() ? "N/A" : status.asText())
                + ", msg=" + (msg.isMissingNode() ? "N/A" : msg.asText())
                + ", data=" + dataSummary;
    }

    private static Iterable<JsonNode> iterable(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        return node;
    }

    private static void addReturn(List<EtfDetailInfo.PeriodReturn> returns, String period, JsonNode node, String field) {
        BigDecimal value = decimal(node, field);
        if (value != null) {
            returns.add(new EtfDetailInfo.PeriodReturn(period, value));
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.path(field);
        return value == null || value.isMissingNode() || value.isNull() ? "" : value.asText();
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.path(field);
        if (value == null || value.isMissingNode() || value.isNull() || value.asText().isBlank()) {
            return null;
        }
        return new BigDecimal(value.asText());
    }

    private static BigDecimal firstNonNull(BigDecimal first, BigDecimal second) {
        return first != null ? first : second;
    }

    private static String marketOf(String code) {
        if (code == null) {
            return "";
        }
        if (code.endsWith(".SZ")) {
            return "SZ";
        }
        if (code.endsWith(".SH")) {
            return "SH";
        }
        return "";
    }
}
