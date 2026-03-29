package com.minhhai.wms.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsIssueDetailDTO {

    private Integer giDetailId;
    private String productDisplayName;
    private String uom;
    private Integer orderedQty; // remaining qty to issue
    private Integer issuedQty;  // actual qty issued (input from form)
    private String batchNumber;
    private String binLocation;
}
