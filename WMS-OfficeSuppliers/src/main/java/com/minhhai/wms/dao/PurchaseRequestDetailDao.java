package com.minhhai.wms.dao;

import com.minhhai.wms.entity.PurchaseRequestDetail;

import java.util.List;

public interface PurchaseRequestDetailDao {

    List<PurchaseRequestDetail> findByPrId(Integer prId);

    PurchaseRequestDetail save(PurchaseRequestDetail detail);

    void deleteByPrId(Integer prId);

    void updatePoDetailId(Integer prDetailId, Integer poDetailId);
}
