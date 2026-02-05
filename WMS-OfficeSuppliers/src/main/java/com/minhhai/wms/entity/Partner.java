package com.minhhai.wms.entity;

import jakarta.persistence.*;
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

    @Column(name = "PartnerName", length = 200, nullable = false)
    private String partnerName;

    @Column(name = "PartnerType", length = 20, nullable = false)
    @Pattern(regexp = "^(Supplier|Customer)$", message = "Partner Type phải là 'Supplier' hoặc 'Customer'")
    private String partnerType; // Supplier, Customer

    @Column(name = "ContactPerson", length = 100)
    private String contactPerson;

    @Column(name = "PhoneNumber", length = 20)
    private String phoneNumber;

    @OneToMany(mappedBy = "supplier") // Phía PO gọi partner là supplier
    private List<PurchaseOrder> purchaseOrders;

    @OneToMany(mappedBy = "customer") // Phía SO gọi partner là customer
    private List<SalesOrder> salesOrders;
}