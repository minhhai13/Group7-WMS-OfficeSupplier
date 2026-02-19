package com.minhhai.wms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "Partners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Partner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PartnerID")
    private Integer partnerId;

    @NotBlank(message = "Partner name is required")
    @Column(name = "PartnerName", length = 200, nullable = false)
    private String partnerName;

    @NotBlank(message = "Partner type is required")
    @Column(name = "PartnerType", length = 20, nullable = false)
    @Pattern(regexp = "^(Supplier|Customer)$", message = "Partner type must be 'Supplier' or 'Customer'")
    private String partnerType; // Supplier, Customer

    @Column(name = "ContactPerson", length = 100)
    private String contactPerson;

    @Column(name = "PhoneNumber", length = 20)
    private String phoneNumber;

    @Builder.Default
    @Column(name = "IsActive", nullable = false, columnDefinition = "bit default 1")
    private Boolean isActive = true;

    @OneToMany(mappedBy = "supplier") // Phía PO gọi partner là supplier
    private List<PurchaseOrder> purchaseOrders;

    @OneToMany(mappedBy = "customer") // Phía SO gọi partner là customer
    private List<SalesOrder> salesOrders;
}