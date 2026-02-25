package com.minhhai.wms.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseRequestDTO {
    private Integer prId;
    private String prNumber;
    private String warehouseName;
    private String status;
    private String relatedSONumber;
    private String poNumber;
    private List<PurchaseRequestDetailDTO> details;
}
