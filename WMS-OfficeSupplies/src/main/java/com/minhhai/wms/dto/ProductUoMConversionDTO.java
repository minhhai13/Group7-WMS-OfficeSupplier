package com.minhhai.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUoMConversionDTO {
    
    private Integer conversionId;
    
    private Integer productId;
    
    private String fromUoM;
    
    private String toUoM;
    
    private Integer conversionFactor;
}
