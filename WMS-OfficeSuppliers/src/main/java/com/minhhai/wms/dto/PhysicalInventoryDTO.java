package com.minhhai.wms.dto; // Hoặc package com.minhhai.wms.dto.report tùy cấu trúc của bro

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhysicalInventoryDTO {
    private String skuCode;
    private String productName;
    private String binLocation;
    private String batchNumber;
    private Integer onHandQty;
    private String uom; // THÊM TRƯỜNG NÀY ĐỂ HIỂN THỊ ĐƠN VỊ TÍNH
}