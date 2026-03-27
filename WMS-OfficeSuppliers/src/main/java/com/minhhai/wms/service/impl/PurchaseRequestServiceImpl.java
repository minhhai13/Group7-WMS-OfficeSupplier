package com.minhhai.wms.service.impl;

import com.minhhai.wms.dao.*;
import com.minhhai.wms.dto.PurchaseRequestDTO;
import com.minhhai.wms.dto.PurchaseRequestDetailDTO;
import com.minhhai.wms.entity.*;
import com.minhhai.wms.service.PurchaseRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseRequestServiceImpl implements PurchaseRequestService {

    private final PurchaseRequestDao prDao;
    private final PurchaseRequestDetailDao prDetailDao;
    private final PurchaseOrderDao poDao;
    private final PurchaseOrderDetailDao poDetailDao;
    private final PartnerDao partnerDao;

    // ==================== Query ====================

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseRequestDTO> getPRsByWarehouse(Integer warehouseId, String status) {
        List<PurchaseRequest> prs;
        if (status != null && !status.isBlank() && !"All".equals(status)) {
            prs = prDao.findByWarehouseIdAndStatus(warehouseId, status);
        } else {
            prs = prDao.findByWarehouseId(warehouseId);
        }
        // Manual fetch details for each PR
        for (PurchaseRequest pr : prs) {
            pr.setDetails(prDetailDao.findByPrId(pr.getPrId()));
        }
        return prs.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseRequestDTO> getApprovedPRsForConversion(Integer warehouseId) {
        List<PurchaseRequest> prs = prDao.findByWarehouseIdAndStatus(warehouseId, "Approved");
        for (PurchaseRequest pr : prs) {
            pr.setDetails(prDetailDao.findByPrId(pr.getPrId()));
        }
        return prs.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    // ==================== Convert PRs to PO ====================

    @Override
    public String convertPRsToPO(List<Integer> prIds, Integer supplierId, User user) {
        if (prIds == null || prIds.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một Purchase Request.");
        }

        Partner supplier = partnerDao.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + supplierId));

        // Load and validate all selected PRs
        List<PurchaseRequest> selectedPRs = new ArrayList<>();
        for (Integer prId : prIds) {
            PurchaseRequest pr = prDao.findById(prId)
                    .orElseThrow(() -> new IllegalArgumentException("PR not found: " + prId));
            if (!"Approved".equals(pr.getStatus())) {
                throw new IllegalArgumentException(
                        "PR " + pr.getPrNumber() + " is not in Approved status.");
            }
            // Manual fetch PR details
            pr.setDetails(prDetailDao.findByPrId(pr.getPrId()));
            selectedPRs.add(pr);
        }

        // Create PO – PO from PR goes directly to Pending Approval (not Draft)
        String poNumber = generatePONumber();
        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber(poNumber);
        Warehouse warehouse = new Warehouse();
        warehouse.setWarehouseId(user.getWarehouse().getWarehouseId());
        po.setWarehouse(warehouse);
        po.setSupplier(supplier);
        po.setPoStatus("Pending Approval");

        // Save PO header first to get generated POID
        PurchaseOrder savedPo = poDao.save(po);

        // Group PR details by product → aggregate quantities
        Map<Integer, List<PurchaseRequestDetail>> detailsByProduct = new LinkedHashMap<>();
        for (PurchaseRequest pr : selectedPRs) {
            if (pr.getDetails() != null) {
                for (PurchaseRequestDetail prDetail : pr.getDetails()) {
                    detailsByProduct.computeIfAbsent(
                            prDetail.getProduct().getProductId(), k -> new ArrayList<>())
                            .add(prDetail);
                }
            }
        }

        // Create PO details (one per product, aggregated qty) and save
        Map<Integer, PurchaseOrderDetail> productToPoDetail = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<PurchaseRequestDetail>> entry : detailsByProduct.entrySet()) {
            List<PurchaseRequestDetail> prDetails = entry.getValue();
            PurchaseRequestDetail first = prDetails.get(0);

            int totalQty = prDetails.stream().mapToInt(PurchaseRequestDetail::getRequestedQty).sum();

            PurchaseOrderDetail poDetail = new PurchaseOrderDetail();
            poDetail.setPurchaseOrder(savedPo);
            poDetail.setProduct(first.getProduct());
            poDetail.setOrderedQty(totalQty);
            poDetail.setReceivedQty(0);
            poDetail.setUom(first.getUom());

            PurchaseOrderDetail savedPoDetail = poDetailDao.save(poDetail);
            productToPoDetail.put(first.getProduct().getProductId(), savedPoDetail);
        }

        // Link PRDetails to PODetails + update PR status to Converted
        for (PurchaseRequest pr : selectedPRs) {
            pr.setStatus("Converted");
            // Link PO to PR (need to build lightweight PO reference)
            pr.setPurchaseOrder(savedPo);
            prDao.save(pr);

            if (pr.getDetails() != null) {
                for (PurchaseRequestDetail prDetail : pr.getDetails()) {
                    PurchaseOrderDetail matchedPODetail =
                            productToPoDetail.get(prDetail.getProduct().getProductId());
                    if (matchedPODetail != null) {
                        prDetail.setPurchaseOrderDetail(matchedPODetail);
                        prDetailDao.save(prDetail);
                    }
                }
            }
        }

        return poNumber;
    }

    // ==================== PO Number Generation ====================

    private String generatePONumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "PO-" + dateStr + "-";
        String maxNumber = poDao.findMaxPoNumber(prefix);
        int nextNum = (maxNumber != null)
                ? Integer.parseInt(maxNumber.substring(prefix.length())) + 1 : 1;
        return prefix + String.format("%03d", nextNum);
    }

    // ==================== Mapping ====================

    private PurchaseRequestDTO mapToDTO(PurchaseRequest pr) {
        PurchaseRequestDTO dto = PurchaseRequestDTO.builder()
                .prId(pr.getPrId())
                .prNumber(pr.getPrNumber())
                .warehouseName(pr.getWarehouse() != null ? pr.getWarehouse().getWarehouseName() : null)
                .status(pr.getStatus())
                .relatedSONumber(pr.getRelatedSalesOrder() != null
                        ? pr.getRelatedSalesOrder().getSoNumber() : null)
                .poNumber(pr.getPurchaseOrder() != null
                        ? pr.getPurchaseOrder().getPoNumber() : null)
                .build();

        if (pr.getDetails() != null) {
            List<PurchaseRequestDetailDTO> detailDTOs = pr.getDetails().stream()
                    .map(this::mapDetailToDTO).collect(Collectors.toList());
            dto.setDetails(detailDTOs);
        }
        return dto;
    }

    private PurchaseRequestDetailDTO mapDetailToDTO(PurchaseRequestDetail detail) {
        String displayName = detail.getProduct() != null
                ? detail.getProduct().getSku() + " - " + detail.getProduct().getProductName() : "";
        return PurchaseRequestDetailDTO.builder()
                .prDetailId(detail.getPrDetailId())
                .productId(detail.getProduct() != null ? detail.getProduct().getProductId() : null)
                .productDisplayName(displayName)
                .uom(detail.getUom())
                .requestedQty(detail.getRequestedQty())
                .build();
    }
}
