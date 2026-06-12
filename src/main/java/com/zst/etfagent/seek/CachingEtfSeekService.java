package com.zst.etfagent.seek;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zst.etfagent.config.EtfAgentProperties;
import com.zst.etfagent.domain.EtfDetailInfo;
import com.zst.etfagent.domain.EtfFundInfo;
import com.zst.etfagent.domain.IndexDetailInfo;
import com.zst.etfagent.domain.IndexInfo;
import com.zst.etfagent.domain.SectorInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

@Service
public class CachingEtfSeekService {

    private static final Logger log = LoggerFactory.getLogger(CachingEtfSeekService.class);

    private final EtfSeekClient client;
    private final Cache<String, Object> cache;

    public CachingEtfSeekService(EtfSeekClient client, EtfAgentProperties properties) {
        this.client = client;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(properties.getCacheTtlSeconds()))
                .maximumSize(10_000)
                .build();
    }

    public List<SectorInfo> searchSectors(String keyword) {
        return cached("W1055046", "searchContent=" + keyword, "W1055046:" + keyword, () -> client.searchSectors(keyword));
    }

    public List<IndexInfo> listIndexesBySector(String sectorCode) {
        return cached("W1055048", "sSectcode=" + sectorCode, "W1055048:" + sectorCode, () -> client.listIndexesBySector(sectorCode));
    }

    public List<EtfFundInfo> listEtfsByIndex(String indexCode) {
        return cached("W1055011", "sIrdCode=" + indexCode, "W1055011:" + indexCode, () -> client.listEtfsByIndex(indexCode));
    }

    public EtfDetailInfo getEtfDetail(String fundCode) {
        return cached("ETF_DETAIL", "sIrdCode=" + fundCode, "ETF_DETAIL:" + fundCode, () -> client.getEtfDetail(fundCode));
    }

    public IndexDetailInfo getIndexDetail(String indexCode) {
        return cached("INDEX_DETAIL", "indexCode=" + indexCode, "INDEX_DETAIL:" + indexCode, () -> client.getIndexDetail(indexCode));
    }

    @SuppressWarnings("unchecked")
    private <T> T cached(String api, String params, String key, Supplier<T> loader) {
        Object existing = cache.getIfPresent(key);
        if (existing != null) {
            log.info("[ETF-SEEK][CACHE-HIT] api={} params={} result={}", api, params, summarize(existing));
            return (T) existing;
        }
        long started = System.currentTimeMillis();
        log.info("[ETF-SEEK][CALL] api={} params={}", api, params);
        T loaded = loader.get();
        cache.put(key, loaded);
        log.info("[ETF-SEEK][DONE] api={} params={} durationMs={} result={}",
                api, params, System.currentTimeMillis() - started, summarize(loaded));
        return loaded;
    }

    private static String summarize(Object value) {
        if (value instanceof List<?> list) {
            return "list(size=" + list.size() + ")";
        }
        if (value == null) {
            return "null";
        }
        return value.getClass().getSimpleName();
    }
}
