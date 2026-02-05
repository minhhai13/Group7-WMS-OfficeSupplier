package com.minhhai.wms.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ProductUoMConversions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"ProductID", "FromUoM", "ToUoM"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductUoMConversion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ConversionID")
    private Integer conversionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ProductID", nullable = false)
    private Product product;

    @Column(name = "FromUoM", length = 10, nullable = false)
    private String fromUoM;

    @Column(name = "ToUoM", length = 10, nullable = false)
    private String toUoM;

    @Column(
            name = "ConversionFactor",
            nullable = false,
            columnDefinition = "int CHECK (ConversionFactor > 0)"
    )
    private Integer conversionFactor; // 1 FromUoM = ConversionFactor × ToUoM
}