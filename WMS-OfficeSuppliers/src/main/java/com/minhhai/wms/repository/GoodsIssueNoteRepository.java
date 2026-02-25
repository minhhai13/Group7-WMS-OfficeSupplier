package com.minhhai.wms.repository;

import com.minhhai.wms.entity.GoodsIssueNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoodsIssueNoteRepository extends JpaRepository<GoodsIssueNote, Integer> {

    List<GoodsIssueNote> findByWarehouse_WarehouseId(Integer warehouseId);

    List<GoodsIssueNote> findByWarehouse_WarehouseIdAndGiStatus(Integer warehouseId, String giStatus);

    @Query("SELECT MAX(g.ginNumber) FROM GoodsIssueNote g WHERE g.ginNumber LIKE :prefix%")
    String findMaxGinNumber(@Param("prefix") String prefix);
}
