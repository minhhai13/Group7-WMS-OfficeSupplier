package com.minhhai.wms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleOrderDetailDTO {

    private Integer soDetailId;

    @NotNull(message = "Choose product")
    private Integer productId;

    private String productDisplayName; // "SKU - ProductName" for display

    @NotBlank(message = "Choose UoM")
    private String uom;

    @NotNull(message = "Quantity must not null")
    @Positive(message = "Quantity must be positive")
    private Integer orderedQty;
}
