package com.minhhai.wms.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseRequestDetailDTO {
    private Integer prDetailId;
    private Integer productId;
    private String productDisplayName;
    private String uom;
    private Integer requestedQty;
}
