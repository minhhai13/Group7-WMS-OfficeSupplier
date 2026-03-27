package com.minhhai.wms.dao;

import com.minhhai.wms.entity.PurchaseOrderDetail;

import java.util.List;

public interface PurchaseOrderDetailDao {

    List<PurchaseOrderDetail> findByPoId(Integer poId);

    PurchaseOrderDetail save(PurchaseOrderDetail detail);

    void deleteByPoId(Integer poId);

    void updateReceivedQty(Integer poDetailId, Integer receivedQty);
}
