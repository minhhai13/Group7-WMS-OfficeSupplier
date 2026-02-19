package com.minhhai.wms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "PurchaseRequests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PRID")
    private Integer prId;

    @Column(name = "PRNumber", length = 20, nullable = false, unique = true, updatable = false)
    private String prNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WarehouseID", nullable = false)
    private Warehouse warehouse;

    @Builder.Default
    @Column(
            name = "Status",
            length = 20,
            columnDefinition = "nvarchar(20) default 'Pending' CHECK ([Status] IN ('Pending', 'Approved', 'Rejected', 'Converted', 'Completed'))"
    )
    private String status = "Pending"; // Pending, Approved, Rejected, Converted, Completed

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RelatedSOID", unique = true)
    private SalesOrder relatedSalesOrder;

    @OneToMany(mappedBy = "purchaseRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PurchaseRequestDetail> details;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "POID")
    private PurchaseOrder purchaseOrder;
}