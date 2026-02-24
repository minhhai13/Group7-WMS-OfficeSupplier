package com.minhhai.wms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderDTO {
    private Integer poId;
    private String poNumber;
    
    @NotNull(message = "Supplier is required")
    private Integer supplierId;
    private String supplierName;
    
    private Integer warehouseId; // from staff's assigned warehouse
    private String warehouseName;
    
    private String poStatus;
    
    @Valid
    @Builder.Default
    private List<PurchaseOrderDetailDTO> details = new ArrayList<>();
}
