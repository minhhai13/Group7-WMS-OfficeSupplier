package com.minhhai.wms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderDetailDTO {
    private Integer poDetailId;
    
    @NotNull(message = "Product is required")
    private Integer productId;
    private String productSku;
    private String productName;
    
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be greater than 0")
    private Integer orderedQty;
    
    private Integer receivedQty; // Read-only for PO Form
    
    @NotBlank(message = "UoM is required")
    private String uom;
}
