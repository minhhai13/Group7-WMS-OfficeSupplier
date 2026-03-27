package com.minhhai.wms.dao;

import com.minhhai.wms.entity.TransferOrder;

import java.util.List;
import java.util.Optional;

public interface TransferOrderDao {

    // -- Queries for outgoing view (destination warehouse perspective) --
    List<TransferOrder> findByDestinationWarehouseId(Integer destWarehouseId);
    List<TransferOrder> findByDestinationWarehouseIdAndStatus(Integer destWarehouseId, String status);
    List<TransferOrder> findBySourceAndDestinationWarehouseId(Integer sourceWarehouseId, Integer destWarehouseId);
    List<TransferOrder> findBySourceAndDestinationWarehouseIdAndStatus(Integer sourceWarehouseId, String status, Integer destWarehouseId);

    // -- Queries for incoming view (source warehouse perspective) --
    List<TransferOrder> findBySourceWarehouseId(Integer sourceWarehouseId);
    List<TransferOrder> findBySourceWarehouseIdAndStatus(Integer sourceWarehouseId, String status);

    Optional<TransferOrder> findById(Integer toId);

    TransferOrder save(TransferOrder transferOrder);

    void deleteById(Integer toId);

    void updateStatus(Integer toId, String status);

    void updateStatusAndRejectReason(Integer toId, String status, String rejectReason);

    String findMaxToNumber(String prefix);
}
