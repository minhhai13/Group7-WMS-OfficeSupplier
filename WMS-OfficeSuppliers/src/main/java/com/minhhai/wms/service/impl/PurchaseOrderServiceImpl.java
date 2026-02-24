package com.minhhai.wms.service.impl;

import com.minhhai.wms.dto.PurchaseOrderDTO;
import com.minhhai.wms.dto.PurchaseOrderDetailDTO;
import com.minhhai.wms.entity.*;
import com.minhhai.wms.repository.*;
import com.minhhai.wms.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository poRepository;
    private final PurchaseOrderDetailRepository poDetailRepository;
    private final GoodsReceiptNoteRepository grnRepository;
    private final GoodsReceiptDetailRepository grDetailRepository;
    private final WarehouseRepository warehouseRepository;
    private final PartnerRepository partnerRepository;
    private final ProductRepository productRepository;

    @Override
    public List<PurchaseOrder> getPOsByWarehouse(Integer warehouseId, String status) {
        if (status != null && !status.isEmpty()) {
            return poRepository.findByWarehouse_WarehouseIdAndPoStatusOrderByPoIdDesc(warehouseId, status);
        }
        return poRepository.findByWarehouse_WarehouseIdOrderByPoIdDesc(warehouseId);
    }

    @Override
    public PurchaseOrder getPOById(Integer poId) {
        return poRepository.findById(poId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid PO ID: " + poId));
    }

    @Override
    @Transactional
    public PurchaseOrder savePO(PurchaseOrderDTO dto, Integer warehouseId, boolean isDraft) {
        PurchaseOrder po;
        if (dto.getPoId() != null) {
            po = getPOById(dto.getPoId());
            if (!po.getWarehouse().getWarehouseId().equals(warehouseId)) {
                throw new SecurityException("Unauthorized access to PO");
            }
            if (!"Draft".equals(po.getPoStatus()) && !"Rejected".equals(po.getPoStatus())) {
                throw new IllegalStateException("Only Draft or Rejected POs can be edited");
            }
            // Clear existing details to be replaced by new ones from DTO
            poDetailRepository.deleteAll(po.getDetails());
            po.getDetails().clear();
        } else {
            po = new PurchaseOrder();
            po.setPoNumber(generatePONumber());
            po.setWarehouse(warehouseRepository.findById(warehouseId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Warehouse ID")));
        }

        po.setSupplier(partnerRepository.findById(dto.getSupplierId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid Supplier ID")));
        
        po.setPoStatus(isDraft ? "Draft" : "Pending Approval");

        List<PurchaseOrderDetail> details = new ArrayList<>();
        for (PurchaseOrderDetailDTO detailDTO : dto.getDetails()) {
            PurchaseOrderDetail detail = new PurchaseOrderDetail();
            detail.setPurchaseOrder(po);
            detail.setProduct(productRepository.findById(detailDTO.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Product ID")));
            detail.setOrderedQty(detailDTO.getOrderedQty());
            detail.setReceivedQty(0);
            detail.setUom(detailDTO.getUom());
            details.add(detail);
        }
        po.setDetails(details);

        return poRepository.save(po);
    }

    @Override
    @Transactional
    public void submitPOForApproval(Integer poId, Integer warehouseId) {
        PurchaseOrder po = getPOById(poId);
        if (!po.getWarehouse().getWarehouseId().equals(warehouseId)) {
            throw new SecurityException("Unauthorized");
        }
        if (!"Draft".equals(po.getPoStatus()) && !"Rejected".equals(po.getPoStatus())) {
            throw new IllegalStateException("Only Draft or Rejected POs can be submitted");
        }
        po.setPoStatus("Pending Approval");
        poRepository.save(po);
    }

    @Override
    @Transactional
    public void approvePO(Integer poId, Integer warehouseId) {
        PurchaseOrder po = getPOById(poId);
        if (!po.getWarehouse().getWarehouseId().equals(warehouseId)) {
            throw new SecurityException("Unauthorized");
        }
        if (!"Pending Approval".equals(po.getPoStatus())) {
            throw new IllegalStateException("Only Pending Approval POs can be approved");
        }
        po.setPoStatus("Approved");
        poRepository.save(po);

        // Generate 1 linked GoodsReceiptNote
        GoodsReceiptNote grn = new GoodsReceiptNote();
        grn.setGrnNumber(generateGRNNumber());
        grn.setPurchaseOrder(po);
        grn.setWarehouse(po.getWarehouse());
        grn.setGrStatus("Draft");
        
        List<GoodsReceiptDetail> grnDetails = new ArrayList<>();
        for (PurchaseOrderDetail poDetail : po.getDetails()) {
            GoodsReceiptDetail grDetail = new GoodsReceiptDetail();
            grDetail.setGoodsReceiptNote(grn);
            grDetail.setPurchaseOrderDetail(poDetail);
            grDetail.setProduct(poDetail.getProduct());
            grDetail.setReceivedQty(0); // will be updated later by storekeeper
            grDetail.setUom(poDetail.getUom());
            // batchNumber and bin will be assigned by storekeeper later, but the entity requires them to be non-null. 
            // We should allow them to be null initially? Wait, GoodsReceiptDetail has:
            // @Column(name = "BatchNumber", length = 50, nullable = false)
            // @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "BinID", nullable = false)
            // If they are not nullable, we can't create GRN details in advance, OR we can just avoid creating GRN details until the storekeeper actually receives them, OR we can set dummy values?
            // Wait, the storekeeper needs to split bins anyway! So the storekeeper will create the GoodsReceiptDetail records when receiving. 
            // So on approve, we ONLY create the GoodsReceiptNote header!
        }
        
        grnRepository.save(grn);
    }

    @Override
    @Transactional
    public void rejectPO(Integer poId, Integer warehouseId) {
        PurchaseOrder po = getPOById(poId);
        if (!po.getWarehouse().getWarehouseId().equals(warehouseId)) {
            throw new SecurityException("Unauthorized");
        }
        if (!"Pending Approval".equals(po.getPoStatus())) {
            throw new IllegalStateException("Only Pending Approval POs can be rejected");
        }
        po.setPoStatus("Rejected");
        poRepository.save(po);
    }

    @Override
    @Transactional
    public void deletePO(Integer poId, Integer warehouseId) {
        PurchaseOrder po = getPOById(poId);
        if (!po.getWarehouse().getWarehouseId().equals(warehouseId)) {
            throw new SecurityException("Unauthorized");
        }
        if (!"Draft".equals(po.getPoStatus()) && !"Rejected".equals(po.getPoStatus())) {
            throw new IllegalStateException("Only Draft or Rejected POs can be deleted");
        }
        poRepository.delete(po);
    }

    private String generatePONumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "PO-" + dateStr + "-";
        long count = poRepository.count(); // simplistic generation
        return prefix + String.format("%03d", count + 1);
    }

    private String generateGRNNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "GRN-" + dateStr + "-";
        long count = grnRepository.count();
        return prefix + String.format("%03d", count + 1);
    }
}
