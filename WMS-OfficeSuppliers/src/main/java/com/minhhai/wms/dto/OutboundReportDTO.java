package com.minhhai.wms.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutboundReportDTO {
    private LocalDateTime outboundDate;
    private String ginNumber;
    private String skuCode;
    private String productName;
    private Integer issuedQuantity;
    private String sourceBin;
    private String batchNumber;
}