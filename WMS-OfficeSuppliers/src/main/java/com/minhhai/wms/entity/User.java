package com.minhhai.wms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "Users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserID")
    private Integer userId;

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(max = 50)
    @Column(name = "Username", length = 50, nullable = false, unique = true)
    private String username;

    @Column(name = "PasswordHash", length = 255, nullable = false)
    private String passwordHash;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100)
    @Column(name = "FullName", length = 100, nullable = false)
    private String fullName;

    @NotBlank(message = "Vai trò không được để trống")
    @Column(
            name = "Role",
            length = 30,
            nullable = false,
            columnDefinition = "nvarchar(30) CHECK ([Role] IN ('System Admin', 'Warehouse Admin', 'Warehouse Manager', 'Purchasing Manager', 'Purchasing Staff', 'Sales Manager', 'Sales Staff', 'Storekeeper'))"
    )
    private String role; // 'System Admin', 'Warehouse Admin', 'Warehouse Manager', 'Purchasing Manager', 'Purchasing Staff',
                        //'Sales Manager', 'Sales Staff', 'Storekeeper'

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WarehouseID")
    private Warehouse warehouse;

}