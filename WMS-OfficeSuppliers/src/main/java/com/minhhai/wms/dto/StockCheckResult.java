package com.minhhai.wms.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockCheckResult {
    private boolean success;
    private boolean hasShortage;
    private Integer soId;
    private String prNumber;   // non-null if PR was created
    private List<ShortageItem> shortages;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShortageItem {
        private String productName;
        private String sku;
        private int orderedQty;
        private int availableQty;
        private int missingQty;
        private String uom;
    }
}
