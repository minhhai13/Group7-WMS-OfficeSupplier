package com.minhhai.wms.service;

import com.minhhai.wms.dto.TransferRequestDTO;
import com.minhhai.wms.entity.User;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

public interface TransferRequestService {
    //idk
    TransferRequestDTO getTRById(Integer id);

    //submit - save draft - delete
    TransferRequestDTO submitTR(@Valid TransferRequestDTO trDTO, User user);

    void saveDraftTR(@Valid TransferRequestDTO trDTO, User user);

    void deleteTR(Integer id, User user);

    //danh sach tr minh gui cho kho khac
    List<TransferRequestDTO> getOutgoingTransferRequests(Integer warehouseId, String status, Integer sourceWarehouseId);

    //danh sach minh duyet cho kho khac gui toi
    List<TransferRequestDTO> getIncomingWarehouseTransferRequests(Integer warehouseId, String filterStatus, Integer destWarehouseId);

    //approve - reject
    void approveTR(Integer id, User user);

    void rejectTR(Integer id, String trim);

    List<Map<String, String>> getAvailableUoMs(Integer productId);
}