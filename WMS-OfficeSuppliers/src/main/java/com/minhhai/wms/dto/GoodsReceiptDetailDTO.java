package com.minhhai.wms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptDetailDTO {
    private Integer grDetailId;
    
    @NotNull
    private Integer poDetailId;
    
    private Integer productId;
    private String productSku;
    private String productName;
    
    private Integer orderedQty;
    private Integer previousReceivedQty; // Tally of what was received before this transaction
    
    @NotNull(message = "Received Quantity is required")
    @Min(value = 1, message = "Received quantity must be at least 1")
    private Integer receivedQty;
    
    private String uom;
    
    @NotNull(message = "Bin is required")
    private Integer binId;
    
    private String batchNumber; // Mặc định hệ thống tự sinh định dạng BATCH-YYYYMMDD-ID
}
