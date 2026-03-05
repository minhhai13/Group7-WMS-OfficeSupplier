package com.minhhai.wms.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InboundReportDTO {

    private LocalDateTime movementDate;
    private String warehouseName;
    private String productName;
    private String sku;
    private String batchNumber;
    private String binLocation;
    private Integer quantity;
    private String uom;
    private Integer balanceAfter;
}
