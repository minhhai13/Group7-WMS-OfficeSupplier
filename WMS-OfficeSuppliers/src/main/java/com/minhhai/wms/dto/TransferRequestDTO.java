package com.minhhai.wms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequestDTO {
    private Integer trId;

    private String transferNumber;

    @NotNull(message = "Vui lòng chọn kho nguồn")
    private Integer sourceWarehouseId;

    private String sourceWarehouseName;

    @NotNull(message = "Vui lòng chọn kho đích")
    private Integer destinationWarehouseId;

    private String destinationWarehouseName;

    private String trStatus;

    private String rejectReason;

    @Valid
    @NotEmpty(message = "Đơn hàng phải có ít nhất một dòng sản phẩm")
    @Builder.Default
    private List<TransferDetailDTO> details = new ArrayList<>();
}