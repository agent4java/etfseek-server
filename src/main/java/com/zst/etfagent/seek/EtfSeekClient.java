package com.zst.etfagent.seek;

import com.zst.etfagent.domain.EtfDetailInfo;
import com.zst.etfagent.domain.EtfFundInfo;
import com.zst.etfagent.domain.IndexDetailInfo;
import com.zst.etfagent.domain.IndexInfo;
import com.zst.etfagent.domain.SectorInfo;

import java.util.List;

public interface EtfSeekClient {

    List<SectorInfo> searchSectors(String keyword);

    List<IndexInfo> listIndexesBySector(String sectorCode);

    List<EtfFundInfo> listEtfsByIndex(String indexCode);

    EtfDetailInfo getEtfDetail(String fundCode);

    IndexDetailInfo getIndexDetail(String indexCode);
}
