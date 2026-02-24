package com.minhhai.wms.service;

import com.minhhai.wms.dto.GoodsReceiptNoteDTO;
import com.minhhai.wms.entity.GoodsReceiptNote;

import java.util.List;

public interface GoodsReceiptNoteService {
    List<GoodsReceiptNote> getGRNsByWarehouse(Integer warehouseId, String status);
    GoodsReceiptNote getGRNById(Integer grnId);
    void processGRN(GoodsReceiptNoteDTO dto, Integer warehouseId);
}
