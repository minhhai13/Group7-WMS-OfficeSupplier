package com.minhhai.wms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "TransferOrders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TOID")
    private Integer toId;

    @Column(name = "TONumber", length = 20, nullable = false, unique = true, updatable = false)
    private String toNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SourceWarehouseID", nullable = false)
    private Warehouse sourceWarehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DestinationWarehouseID", nullable = false)
    private Warehouse destinationWarehouse;

    @Builder.Default
    @Column(
            name = "Status",
            length = 30,
            columnDefinition = "nvarchar(30) default 'Pending' CHECK ([Status] IN ('Pending', 'Approved', 'In-Transit', 'Completed', 'Rejected'))"
    )
    private String status = "Pending";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedBy", nullable = false)
    private User createdBy;

    @Column(name = "RejectReason", length = 500)
    private String rejectReason;

    @OneToMany(mappedBy = "transferOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TransferOrderDetail> details;

    @AssertTrue(message = "Source warehouse must be different from destination warehouse")
    private boolean isDifferentWarehouses() {
        if (sourceWarehouse == null || destinationWarehouse == null) return true;
        return !sourceWarehouse.equals(destinationWarehouse);
    }
}