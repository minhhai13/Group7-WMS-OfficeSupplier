package com.minhhai.wms.repository;

import com.minhhai.wms.entity.GoodsReceiptNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoodsReceiptNoteRepository extends JpaRepository<GoodsReceiptNote, Integer> {
    List<GoodsReceiptNote> findByWarehouse_WarehouseIdOrderByGrnIdDesc(Integer warehouseId);
    List<GoodsReceiptNote> findByWarehouse_WarehouseIdAndGrStatusOrderByGrnIdDesc(Integer warehouseId, String grStatus);
    boolean existsByGrnNumber(String grnNumber);
}
