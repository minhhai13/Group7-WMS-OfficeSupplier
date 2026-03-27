package com.minhhai.wms.dao;

import com.minhhai.wms.entity.TransferNote;

import java.util.List;
import java.util.Optional;

public interface TransferNoteDao {

    List<TransferNote> findByWarehouseId(Integer warehouseId);

    Optional<TransferNote> findById(Integer tnId);

    TransferNote save(TransferNote transferNote);

    void updateStatus(Integer tnId, String status);

    String findMaxTnNumber(String prefix);
}
