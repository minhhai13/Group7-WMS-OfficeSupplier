package com.minhhai.wms.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptDetailDTO {

    private Integer grDetailId;
    private String productDisplayName;
    private String uom;
    private Integer orderedQty;   // from PO detail — read-only
    private String batchNumber;   // read-only
    private String binLocation;   // read-only

    @NotNull(message = "Số lượng thực nhận không được để trống")
    @PositiveOrZero(message = "Số lượng thực nhận không được âm")
    private Integer receivedQty;  // editable by Storekeeper
}
