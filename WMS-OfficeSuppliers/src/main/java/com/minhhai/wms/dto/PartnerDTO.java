package com.minhhai.wms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerDTO {

    private Integer partnerId;

    @NotBlank(message = "Partner name is required")
    @Size(max = 200, message = "Partner name must be less than 200 characters")
    private String partnerName;

    @NotBlank(message = "Partner type is required")
    @Pattern(regexp = "^(Supplier|Customer)$", message = "Partner type must be 'Supplier' or 'Customer'")
    private String partnerType;

    @Size(max = 100, message = "Contact person must be less than 100 characters")
    private String contactPerson;

    @Pattern(regexp = "^(0|\\+84)[0-9]{8,9}$", message = "Phone number must be valid (10-11 digits, starts with 0 or +84)")
    @Size(max = 20, message = "Phone number must be less than 20 characters")
    private String phoneNumber;
    
    private Boolean isActive;
}
