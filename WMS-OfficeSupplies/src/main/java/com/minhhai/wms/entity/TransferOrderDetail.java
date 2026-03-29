package com.minhhai.wms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Entity
@Table(name = "TransferOrderDetails")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferOrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TODetailID")
    private Integer toDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TOID", nullable = false)
    private TransferOrder transferOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductID", nullable = false)
    private Product product;

    @Positive(message = "Requested quantity must be greater than 0")
    @Column(name = "RequestedQty", nullable = false)
    private Integer requestedQty;

    @Builder.Default
    @Column(name = "IssuedQty")
    private Integer issuedQty = 0;

    @Builder.Default
    @Column(name = "ReceivedQty")
    private Integer receivedQty = 0;

    @Column(name = "UoM", length = 10, nullable = false)
    private String uom;
}