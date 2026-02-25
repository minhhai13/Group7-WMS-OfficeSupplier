package com.minhhai.wms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "SalesOrderDetails")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesOrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SODetailID")
    private Integer soDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SOID", nullable = false)
    private SalesOrder salesOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductID", nullable = false)
    private Product product;

    @Positive(message = "Ordered quantity must be greater than 0")
    @Column(name = "OrderedQty", nullable = false)
    private Integer orderedQty;

    @PositiveOrZero
    @Builder.Default
    @Column(name = "IssuedQty", columnDefinition = "int default 0")
    private Integer issuedQty = 0;

    @Column(name = "UoM", length = 10, nullable = false)
    private String uom;

    @OneToMany(mappedBy = "salesOrderDetail", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GoodsIssueDetail> goodsIssueDetails;

    // Thêm vào trong class SalesOrderDetail
    @OneToOne(mappedBy = "salesOrderDetail")
    private PurchaseRequestDetail purchaseRequestDetail;
}