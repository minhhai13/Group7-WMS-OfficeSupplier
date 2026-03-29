package com.minhhai.wms.dao;

import com.minhhai.wms.entity.GoodsIssueNote;

import java.util.List;
import java.util.Optional;

public interface GoodsIssueNoteDao {

    List<GoodsIssueNote> findByWarehouseId(Integer warehouseId);

    List<GoodsIssueNote> findByWarehouseIdAndStatus(Integer warehouseId, String status);

    Optional<GoodsIssueNote> findById(Integer ginId);

    GoodsIssueNote save(GoodsIssueNote gin);

    void updateStatus(Integer ginId, String status);

    String findMaxGinNumber(String prefix);

    boolean existsBySalesOrderId(Integer soId);
}
