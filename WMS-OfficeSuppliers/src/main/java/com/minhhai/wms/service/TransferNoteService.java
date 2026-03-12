package com.minhhai.wms.service;

import com.minhhai.wms.dto.TransferNoteDTO;
import com.minhhai.wms.entity.User;

import java.util.List;
import java.util.Map;

public interface TransferNoteService {
    List<TransferNoteDTO> getTransferNotes(Integer warehouseId);
    TransferNoteDTO getTransferNoteById(Integer tnId);
    void createTransferNote(TransferNoteDTO dto, User currentUser);
    void completeTransferNote(Integer tnId, User storekeeper);
}
