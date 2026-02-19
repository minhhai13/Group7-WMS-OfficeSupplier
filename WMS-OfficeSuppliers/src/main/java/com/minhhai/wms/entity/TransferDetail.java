package com.minhhai.wms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Entity
@Table(name = "TransferDetails")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TDetailID")
    private Integer tDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TransferID", nullable = false)
    private Transfer transfer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductID", nullable = false)
    private Product product;

    @Positive(message = "Quantity must be greater than 0")
    @Column(name = "Quantity", nullable = false)
    private Integer quantity;

    @Column(name = "UoM", length = 10, nullable = false)
    private String uom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FromBinID")
    private Bin fromBin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ToBinID")
    private Bin toBin;

    @Column(name = "BatchNumber", length = 50)
    private String batchNumber;
}