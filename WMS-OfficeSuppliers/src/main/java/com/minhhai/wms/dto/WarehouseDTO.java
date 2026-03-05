package com.minhhai.wms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseDTO {

    private Integer warehouseId;

    @NotBlank(message = "Warehouse code is required")
    @Size(max = 20, message = "Warehouse code must be less than 20 characters")
    private String warehouseCode;

    @NotBlank(message = "Warehouse name is required")
    @Size(max = 100, message = "Warehouse name must be less than 100 characters")
    private String warehouseName;

    @Size(max = 255, message = "Address must be less than 255 characters")
    private String address;

    private Boolean isActive;
}
