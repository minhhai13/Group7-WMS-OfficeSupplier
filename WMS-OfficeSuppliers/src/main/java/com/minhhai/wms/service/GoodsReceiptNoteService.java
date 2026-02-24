package com.minhhai.wms.service;

import com.minhhai.wms.dto.GoodsReceiptDetailDTO;
import com.minhhai.wms.dto.GoodsReceiptNoteDTO;

import java.util.List;

public interface GoodsReceiptNoteService {

    List<GoodsReceiptNoteDTO> getGRNsByWarehouse(Integer warehouseId, String status);

    GoodsReceiptNoteDTO getGRNById(Integer grnId);

    /**
     * Post GRN: update stock, log movements, update PO, handle back-order.
     * Returns a result message for flash display.
     */
    String postGRN(Integer grnId, List<GoodsReceiptDetailDTO> receivedDetails);
}
