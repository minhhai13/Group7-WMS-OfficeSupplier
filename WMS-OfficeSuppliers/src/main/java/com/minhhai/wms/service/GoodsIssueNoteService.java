package com.minhhai.wms.service;

import com.minhhai.wms.dto.GoodsIssueDetailDTO;
import com.minhhai.wms.dto.GoodsIssueNoteDTO;

import java.util.List;

public interface GoodsIssueNoteService {

    List<GoodsIssueNoteDTO> getGINsByWarehouse(Integer warehouseId, String status);

    GoodsIssueNoteDTO getGINById(Integer ginId);

    /**
     * Post GIN: issue stock (decrease availableQty & reservedQty), log movements, update SO, handle back-order.
     * Returns a result message for flash display.
     */
    String postGIN(Integer ginId, List<GoodsIssueDetailDTO> issuedDetails);
}
