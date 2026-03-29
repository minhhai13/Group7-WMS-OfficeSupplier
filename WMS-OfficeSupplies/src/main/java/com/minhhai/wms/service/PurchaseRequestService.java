package com.minhhai.wms.service;

import com.minhhai.wms.dto.PurchaseRequestDTO;
import com.minhhai.wms.entity.User;

import java.util.List;

public interface PurchaseRequestService {

    List<PurchaseRequestDTO> getPRsByWarehouse(Integer warehouseId, String status);

    /**
     * Get approved PRs that can be converted to PO.
     * Filters by warehouse and optionally by supplier (based on product matching).
     */
    List<PurchaseRequestDTO> getApprovedPRsForConversion(Integer warehouseId);

    /**
     * Convert selected PRs into a PO.
     * Groups PRDetails by product, creates PO with total quantities.
     * Sets PR status to "Converted" and links PR.POID.
     */
    String convertPRsToPO(List<Integer> prIds, Integer supplierId, User user);
}
