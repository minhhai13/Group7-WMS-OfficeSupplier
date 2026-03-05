package com.minhhai.wms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {

    private Integer productId;

    @NotBlank(message = "SKU is required")
    @Size(max = 50, message = "SKU must be less than 50 characters")
    private String sku;

    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Product name must be less than 200 characters")
    private String productName;

    @DecimalMin(value = "0.0", message = "Unit weight must be positive")
    private BigDecimal unitWeight;

    @NotBlank(message = "Base UoM is required")
    @Size(max = 10, message = "Base UoM must be less than 10 characters")
    private String baseUoM;

    @Min(value = 0, message = "Minimum stock level must be 0 or greater")
    private Integer minStockLevel;

    private Boolean isActive;
}
