package com.minhhai.wms.entity;

import jakarta.persistence.*;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SourceWarehouseID", nullable = false)
    private Warehouse sourceWarehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DestinationWarehouseID", nullable = false)
    private Warehouse destinationWarehouse;

    @Column(
            name = "TransferStatus",
            length = 30,
            columnDefinition = "nvarchar(30) default 'Pending' CHECK ([TransferStatus] IN ('Pending', 'Approved', 'In-Transit', 'Completed'))"
    )
    private String transferStatus = "Pending"; // Pending, Approved, In-Transit, Completed

    @OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TransferDetail> details;
}