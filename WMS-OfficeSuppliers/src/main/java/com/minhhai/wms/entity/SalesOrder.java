package com.minhhai.wms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "SalesOrders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SOID")
    private Integer soId;

    @Column(name = "SONumber", length = 20, nullable = false, unique = true, updatable = false)
    private String soNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WarehouseID", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CustomerID", nullable = false)
    private Partner customer;

    @Column(
            name = "SOStatus",
            length = 30,
            columnDefinition = "nvarchar(30) default 'Pending' CHECK ([SOStatus] IN ('Pending', 'Waiting for stock', 'Waiting for confirm', 'Approved', 'Incomplete', 'Complete', 'Cancelled'))"
    )
    private String soStatus = "Pending"; // Pending, Waiting for stock, Waiting for confirm, Approved, Incomplete, Complete, Cancelled

    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SalesOrderDetail> details;

    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GoodsIssueNote> goodsIssueNotes;

    @OneToOne(mappedBy = "relatedSalesOrder")
    private PurchaseRequest purchaseRequest;
}