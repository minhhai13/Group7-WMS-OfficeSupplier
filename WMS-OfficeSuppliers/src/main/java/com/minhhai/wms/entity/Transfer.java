package com.minhhai.wms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "Transfers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TransferID")
    private Integer transferId;

    @Column(name = "TransferNumber", length = 20, nullable = false, unique = true, updatable = false)
    private String transferNumber;

    @Column(name = "RejectReason", length = 500)
    private String rejectReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SourceWarehouseID", nullable = false)
    private Warehouse sourceWarehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DestinationWarehouseID", nullable = false)
    private Warehouse destinationWarehouse;

    @Builder.Default
    @Column(
            name = "TransferStatus",
            length = 30,
            columnDefinition = "nvarchar(30) default 'Pending' CHECK ([TransferStatus] IN ('Draft', 'Pending', 'Approved', 'In-Transit', 'Completed', 'Rejected'))"
    )
    private String transferStatus = "Pending"; // Pending, Approved, In-Transit, Completed

    @OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TransferDetail> details;

    @AssertTrue(message = "Source warehouse must be different from destination warehouse")
    private boolean isDifferentWarehouses() {
        if (sourceWarehouse == null || destinationWarehouse == null) return true;
        return !sourceWarehouse.equals(destinationWarehouse);
    }
}