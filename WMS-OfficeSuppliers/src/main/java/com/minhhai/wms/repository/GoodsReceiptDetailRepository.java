package com.minhhai.wms.repository;

import com.minhhai.wms.entity.GoodsReceiptDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoodsReceiptDetailRepository extends JpaRepository<GoodsReceiptDetail, Integer> {

    /**
     * Find all GRN details allocated to a specific bin from Draft GRNs.
     * Used for virtual capacity calculation — these items are "reserved" but not yet posted.
     */
    @Query("SELECT d FROM GoodsReceiptDetail d " +
           "WHERE d.bin.binId = :binId " +
           "AND d.goodsReceiptNote.grStatus = 'Draft'")
    List<GoodsReceiptDetail> findDraftGrnDetailsByBinId(@Param("binId") Integer binId);
}
