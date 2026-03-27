package com.minhhai.wms.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrentStockDTO {

    private Integer productId;
    private String  sku;
    private String  productName;
    private String  uom;
    private Integer minStockLevel;

    private int totalAvailable;   // SUM(QtyAvailable)
    private int totalReserved;    // SUM(QtyReserved)   – committed by SO/TO
    private int freeStock;        // totalAvailable - totalReserved (can be sold/transferred)
    private int totalInTransit;   // SUM(QtyInTransit)  – on the way from source WH

    /** true when totalAvailable < minStockLevel */
    public boolean isBelowThreshold() {
        return minStockLevel != null && minStockLevel > 0 && totalAvailable < minStockLevel;
    }
}
