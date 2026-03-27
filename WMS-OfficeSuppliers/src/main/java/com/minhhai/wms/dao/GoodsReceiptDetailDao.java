package com.minhhai.wms.dao;

import com.minhhai.wms.entity.GoodsReceiptDetail;

import java.util.List;

public interface GoodsReceiptDetailDao {

    List<GoodsReceiptDetail> findByGrnId(Integer grnId);

    List<GoodsReceiptDetail> findDraftByBinId(Integer binId);

    GoodsReceiptDetail save(GoodsReceiptDetail detail);

    void updateReceivedQty(Integer grDetailId, Integer receivedQty);
}
