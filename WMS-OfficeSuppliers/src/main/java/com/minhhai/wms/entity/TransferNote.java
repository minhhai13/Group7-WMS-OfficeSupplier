package com.minhhai.wms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "TransferNotes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TNID")
    private Integer tnId;

    @Column(name = "TNNumber", length = 20, nullable = false, unique = true, updatable = false)
    private String tnNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WarehouseID", nullable = false)
    private Warehouse warehouse;

    @Builder.Default
    @Column(
            name = "Status",
            length = 20,
            columnDefinition = "nvarchar(20) default 'Approved' CHECK ([Status] IN ('Approved', 'Completed', 'Cancelled'))"
    )
    private String status = "Approved";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedBy", nullable = false)
    private User createdBy;

    @Builder.Default
    @Column(name = "CreatedAt", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "transferNote", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TransferNoteDetail> details;
}
