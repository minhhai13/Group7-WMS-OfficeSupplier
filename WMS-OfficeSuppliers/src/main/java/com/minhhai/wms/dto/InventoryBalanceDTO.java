package com.minhhai.wms.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryBalanceDTO {

    private Integer productId;
    private String sku;
    private String productName;
    private Integer warehouseId;
    private String warehouseName;
    private String uom;

    private int openingStock;  // Sum(Receipt - Issue) before startDate
    private int inboundQty;    // Sum(Receipt) in [startDate, endDate]
    private int outboundQty;   // Sum(Issue)   in [startDate, endDate]
    private int closingStock;  // openingStock + inboundQty - outboundQty
}
