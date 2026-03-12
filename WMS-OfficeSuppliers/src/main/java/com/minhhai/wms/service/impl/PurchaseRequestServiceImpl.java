package com.minhhai.wms.service.impl;

import com.minhhai.wms.dto.PurchaseRequestDTO;
import com.minhhai.wms.dto.PurchaseRequestDetailDTO;
import com.minhhai.wms.entity.*;
import com.minhhai.wms.repository.*;
import com.minhhai.wms.service.PurchaseRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    private final PurchaseRequestRepository prRepository;
    private final PurchaseOrderRepository poRepository;
    private final PartnerRepository partnerRepository;

    // ==================== Query ====================

    // ==================== Query ====================

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseRequestDTO> getPRsByWarehouse(Integer warehouseId, String status, Pageable pageable) {
        Page<PurchaseRequest> prs;
        if (status != null && !status.isBlank() && !"All".equals(status)) {
            // Sẽ tự động gọi hàm số (2) trong Repository -> trả về Page
            prs = prRepository.findByWarehouse_WarehouseIdAndStatus(warehouseId, status, pageable);
        } else {
            prs = prRepository.findByWarehouse_WarehouseId(warehouseId, pageable);
        }
        return prs.map(this::mapToDTO);
    }
    @Override
    @Transactional(readOnly = true)
    public List<PurchaseRequestDTO> getApprovedPRsForConversion(Integer warehouseId) {
        List<PurchaseRequest> prs = prRepository.findByWarehouse_WarehouseIdAndStatus(warehouseId, "Approved");
        return prs.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    // ==================== Convert PRs to PO ====================

    @Override
    public String convertPRsToPO(List<Integer> prIds, Integer supplierId, User user) {
        if (prIds == null || prIds.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một Purchase Request.");
        }

        Partner supplier = partnerRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + supplierId));

        // Collect all selected PRs
        List<PurchaseRequest> selectedPRs = new ArrayList<>();
        for (Integer prId : prIds) {
            PurchaseRequest pr = prRepository.findById(prId)
                    .orElseThrow(() -> new IllegalArgumentException("PR not found: " + prId));
            if (!"Approved".equals(pr.getStatus())) {
                throw new IllegalArgumentException("PR " + pr.getPrNumber() + " không ở trạng thái Approved.");
            }
            selectedPRs.add(pr);
        }

        // Create PO
        String poNumber = generatePONumber();
        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber(poNumber);
        po.setWarehouse(user.getWarehouse());
        po.setSupplier(supplier);
        po.setPoStatus("Pending Approval");
        po.setDetails(new ArrayList<>());

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

        // Create PO details (one per product, aggregated qty)
        for (Map.Entry<Integer, List<PurchaseRequestDetail>> entry : detailsByProduct.entrySet()) {
            List<PurchaseRequestDetail> prDetails = entry.getValue();
            PurchaseRequestDetail first = prDetails.get(0);

            int totalQty = 0;
            for (PurchaseRequestDetail d : prDetails) {
                totalQty += d.getRequestedQty();
            }

            PurchaseOrderDetail poDetail = new PurchaseOrderDetail();
            poDetail.setPurchaseOrder(po);
            poDetail.setProduct(first.getProduct());
            poDetail.setOrderedQty(totalQty);
            poDetail.setReceivedQty(0);
            poDetail.setUom(first.getUom());
            po.getDetails().add(poDetail);
        }

        po = poRepository.save(po);

        // Link PRDetails to PODetails and update PR status
        // We need to match by product since we aggregated
        for (PurchaseRequest pr : selectedPRs) {
            pr.setStatus("Converted");
            pr.setPurchaseOrder(po);

            if (pr.getDetails() != null) {
                for (PurchaseRequestDetail prDetail : pr.getDetails()) {
                    // Find matching PO detail by product
                    for (PurchaseOrderDetail poDetail : po.getDetails()) {
                        if (poDetail.getProduct().getProductId().equals(
                                prDetail.getProduct().getProductId())) {
                            prDetail.setPurchaseOrderDetail(poDetail);
                            break;
                        }
                    }
                }
            }
            prRepository.save(pr);
        }

        return poNumber;
    }

    // ==================== PO Number Generation ====================

    private String generatePONumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "PO-" + dateStr + "-";
        String maxNumber = poRepository.findMaxPoNumber(prefix);
        int nextNum = 1;
        if (maxNumber != null) {
            String suffix = maxNumber.substring(prefix.length());
            nextNum = Integer.parseInt(suffix) + 1;
        }
        return prefix + String.format("%03d", nextNum);
    }

    // ==================== Mapping ====================

    private PurchaseRequestDTO mapToDTO(PurchaseRequest pr) {
        PurchaseRequestDTO dto = PurchaseRequestDTO.builder()
                .prId(pr.getPrId())
                .prNumber(pr.getPrNumber())
                .warehouseName(pr.getWarehouse() != null ? pr.getWarehouse().getWarehouseName() : null)
                .status(pr.getStatus())
                .relatedSONumber(pr.getRelatedSalesOrder() != null ? pr.getRelatedSalesOrder().getSoNumber() : null)
                .poNumber(pr.getPurchaseOrder() != null ? pr.getPurchaseOrder().getPoNumber() : null)
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
