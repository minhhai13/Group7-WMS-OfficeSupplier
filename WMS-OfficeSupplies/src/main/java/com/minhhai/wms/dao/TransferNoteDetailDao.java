package com.minhhai.wms.dao;

import com.minhhai.wms.entity.TransferNoteDetail;

import java.util.List;

public interface TransferNoteDetailDao {

    List<TransferNoteDetail> findByTnId(Integer tnId);

    TransferNoteDetail save(TransferNoteDetail detail);

    void deleteByTnId(Integer tnId);
}
