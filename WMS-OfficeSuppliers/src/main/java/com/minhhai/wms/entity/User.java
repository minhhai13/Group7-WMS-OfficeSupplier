package com.minhhai.wms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "Users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserID")
    private Integer userId;

    @NotBlank(message = "Username is required")
    @Size(max = 50)
    @Column(name = "Username", length = 50, nullable = false, unique = true)
    private String username;

    @NotBlank(message = "Password hash is required")
    @Column(name = "PasswordHash", length = 255, nullable = false)
    private String passwordHash;

    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    @Column(name = "FullName", length = 100, nullable = false)
    private String fullName;

    @NotBlank(message = "Role is required")
    @Column(
            name = "Role",
            length = 30,
            nullable = false,
            columnDefinition = "nvarchar(30) CHECK ([Role] IN ('System Admin', 'Warehouse Admin', 'Warehouse Manager', 'Purchasing Manager', 'Purchasing Staff', 'Sales Manager', 'Sales Staff', 'Storekeeper'))"
    )
    private String role;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "WarehouseID")
    private Warehouse warehouse;

    @Builder.Default
    @Column(name = "IsActive", nullable = false, columnDefinition = "bit default 1")
    private Boolean isActive = true;
}