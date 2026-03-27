package com.minhhai.wms.dao;

import com.minhhai.wms.entity.TransferOrderDetail;

import java.util.List;

public interface TransferOrderDetailDao {

    List<TransferOrderDetail> findByTransferOrderId(Integer toId);

    TransferOrderDetail save(TransferOrderDetail detail);

    void updateReceivedQty(Integer toDetailId, Integer receivedQty);

    void updateIssuedQty(Integer toDetailId, Integer issuedQty);
}
