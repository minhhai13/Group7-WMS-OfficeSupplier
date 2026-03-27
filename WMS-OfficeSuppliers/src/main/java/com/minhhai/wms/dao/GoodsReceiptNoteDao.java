package com.minhhai.wms.dao;

import com.minhhai.wms.entity.GoodsReceiptNote;

import java.util.List;
import java.util.Optional;

public interface GoodsReceiptNoteDao {

    List<GoodsReceiptNote> findByWarehouseId(Integer warehouseId);

    List<GoodsReceiptNote> findByWarehouseIdAndStatus(Integer warehouseId, String status);

    Optional<GoodsReceiptNote> findById(Integer grnId);

    GoodsReceiptNote save(GoodsReceiptNote grn);

    void updateStatus(Integer grnId, String status);

    String findMaxGrnNumber(String prefix);
}
