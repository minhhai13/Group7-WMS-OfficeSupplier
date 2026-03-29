package com.minhhai.wms.dao;

import com.minhhai.wms.entity.GoodsIssueDetail;

import java.util.List;

public interface GoodsIssueDetailDao {

    List<GoodsIssueDetail> findByGinId(Integer ginId);

    GoodsIssueDetail save(GoodsIssueDetail detail);

    void updateIssuedQty(Integer giDetailId, Integer issuedQty);
}
