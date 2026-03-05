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
public class TransferDetailDTO {
    private Integer transferDetailId;

    @NotNull(message = "Vui lòng chọn sản phẩm")
    private Integer productId;

    private String productDisplayName;

    @NotBlank(message = "Vui lòng chọn đơn vị tính")
    private String uom;

    @NotNull(message = "Số lượng không được để trống")
    @Positive(message = "Số lượng yêu cầu phải lớn hơn 0")
    private Integer transferQty;

    private Integer fromBinId;

    private String fromBinName;

    private Integer toBinId;

    private String toBinName;
}