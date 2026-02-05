package com.minhhai.wms.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PurchaseRequestDetails")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseRequestDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PRDetailID")
    private Integer prDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRID", nullable = false)
    private PurchaseRequest purchaseRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductID", nullable = false)
    private Product product;

    @Column(
            name = "RequestedQty",
            nullable = false,
            columnDefinition = "int CHECK (RequestedQty > 0)"
    )
    private Integer requestedQty;

    @Column(name = "UoM", length = 10, nullable = false, columnDefinition = "nvarchar(10)")
    private String uom;

    @OneToMany(mappedBy = "purchaseRequestDetail", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private java.util.List<PurchaseOrderDetail> purchaseOrderDetails;
}