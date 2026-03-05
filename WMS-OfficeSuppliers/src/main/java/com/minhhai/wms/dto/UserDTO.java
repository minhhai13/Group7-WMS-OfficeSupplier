package com.minhhai.wms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {

    private Integer userId;

    @NotBlank(message = "Username is required")
    @Size(max = 50, message = "Username must be less than 50 characters")
    private String username;

    // Password validation is tricky on edit (optional), handled in service/controller usually, 
    // but for DTO we can keep it simple or use groups. For now, simple.
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must be less than 100 characters")
    private String fullName;

    @NotBlank(message = "Role is required")
    private String role;

    private Integer warehouseId;

    // Add isActive if needed for form binding, though typically handled via toggle
    private Boolean isActive;
}
