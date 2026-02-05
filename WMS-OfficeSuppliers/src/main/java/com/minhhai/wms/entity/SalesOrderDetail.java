package com.minhhai.wms.entity;

import jakarta.persistence.*;
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

    @Column(name = "OrderedQty", nullable = false)
    private Integer orderedQty;

    @Column(name = "IssuedQty", columnDefinition = "int default 0")
    private Integer issuedQty = 0;

    @Column(name = "UoM", length = 10, nullable = false)
    private String uom;

    @OneToMany(mappedBy = "salesOrderDetail", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GoodsIssueDetail> goodsIssueDetails;
}