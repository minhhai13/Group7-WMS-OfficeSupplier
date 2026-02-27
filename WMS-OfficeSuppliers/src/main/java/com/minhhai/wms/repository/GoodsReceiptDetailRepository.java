package com.minhhai.wms.repository;

import com.minhhai.wms.entity.GoodsReceiptDetail;
import com.minhhai.wms.dto.InboundReportDTO; // Nhớ import DTO
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;



import java.util.List;

@Repository
public interface GoodsReceiptDetailRepository extends JpaRepository<GoodsReceiptDetail, Integer> {

    @Query("SELECT d FROM GoodsReceiptDetail d " +
            "WHERE d.bin.binId = :binId " +
            "AND d.goodsReceiptNote.grStatus = 'Draft'")
    List<GoodsReceiptDetail> findDraftGrnDetailsByBinId(@Param("binId") Integer binId);

    // THÊM HÀM NÀY CHO UC28 (Báo cáo Inbound)
    // Sửa lại đường dẫn (bỏ .report)
    @Query("SELECT new com.minhhai.wms.dto.InboundReportDTO(" +
            "sb.arrivalDateTime, grn.grnNumber, p.sku, p.productName, grd.receivedQty, b.binLocation) " +
            "FROM GoodsReceiptDetail grd " +
            "JOIN grd.goodsReceiptNote grn JOIN grd.product p JOIN grd.bin b " +
            "JOIN StockBatch sb ON sb.batchNumber = grd.batchNumber " +
            "WHERE grn.warehouse.warehouseId = :warehouseId AND grn.grStatus = 'Posted' " +
            "ORDER BY sb.arrivalDateTime DESC")
    Page<InboundReportDTO> getInboundReportByWarehouse(@Param("warehouseId") Integer warehouseId, Pageable pageable);
}