package com.minhhai.wms.service.impl;

import com.minhhai.wms.dto.GoodsIssueDetailDTO;
import com.minhhai.wms.dto.GoodsIssueNoteDTO;
import com.minhhai.wms.entity.*;
import com.minhhai.wms.repository.*;
import com.minhhai.wms.service.GoodsIssueNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GoodsIssueNoteServiceImpl implements GoodsIssueNoteService {

    private final GoodsIssueNoteRepository ginRepository;
    private final SalesOrderRepository soRepository;
    private final StockBatchRepository stockBatchRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductUoMConversionRepository uomConversionRepository;

    // ==================== Query ====================

    @Override
    @Transactional(readOnly = true)
    public List<GoodsIssueNoteDTO> getGINsByWarehouse(Integer warehouseId, String status) {
        List<GoodsIssueNote> gins;
        if (status != null && !status.isBlank()) {
            gins = ginRepository.findByWarehouse_WarehouseIdAndGiStatus(warehouseId, status);
        } else {
            gins = ginRepository.findByWarehouse_WarehouseId(warehouseId);
        }
        return gins.stream().map(this::mapToListDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public GoodsIssueNoteDTO getGINById(Integer ginId) {
        GoodsIssueNote gin = ginRepository.findById(ginId)
                .orElseThrow(() -> new IllegalArgumentException("GIN not found: " + ginId));
        return mapToFullDTO(gin);
    }

    // ==================== Post GIN ====================

    @Override
    public String postGIN(Integer ginId, List<GoodsIssueDetailDTO> issuedDetails) {
        GoodsIssueNote gin = ginRepository.findById(ginId)
                .orElseThrow(() -> new IllegalArgumentException("GIN not found: " + ginId));

        if (!"Draft".equals(gin.getGiStatus())) {
            throw new IllegalArgumentException("Chỉ có thể ghi sổ phiếu GIN ở trạng thái Draft.");
        }

        SalesOrder so = gin.getSalesOrder();
        Warehouse warehouse = gin.getWarehouse();

        // Build a lookup map: giDetailId -> issuedQty from form
        Map<Integer, Integer> issuedQtyMap = new HashMap<>();
        for (GoodsIssueDetailDTO dto : issuedDetails) {
            if (dto.getGiDetailId() != null && dto.getIssuedQty() != null) {
                issuedQtyMap.put(dto.getGiDetailId(), dto.getIssuedQty());
            }
        }

        // Process each GIN detail line
        int totalIssuedInput = 0;
        for (GoodsIssueDetail ginDetail : gin.getDetails()) {
            Integer inputIssuedQty = issuedQtyMap.getOrDefault(ginDetail.getGiDetailId(), 0);
            SalesOrderDetail soDetail = ginDetail.getSalesOrderDetail();

            // Validate: issuedQty must be >= 0
            if (inputIssuedQty < 0) {
                throw new IllegalArgumentException("Số lượng thực xuất không được âm.");
            }
            // Validate: issuedQty <= remaining qty (orderedQty - already issued)
            int remainingQty = soDetail.getOrderedQty() - soDetail.getIssuedQty();
            if (inputIssuedQty > remainingQty) {
                throw new IllegalArgumentException(
                        "Số lượng thực xuất không được lớn hơn số lượng còn thiếu ("
                        + remainingQty + ") cho sản phẩm '" + ginDetail.getProduct().getProductName() + "'.");
            }
            totalIssuedInput += inputIssuedQty;

            // Update GIN detail issuedQty
            ginDetail.setIssuedQty(inputIssuedQty);

            if (inputIssuedQty > 0) {
                Product product = ginDetail.getProduct();

                // Convert issuedQty to base UoM for stock
                BigDecimal conversionFactor = getConversionFactor(product, ginDetail.getUom());
                int baseQty = BigDecimal.valueOf(inputIssuedQty)
                        .multiply(conversionFactor).intValue();

                // Find StockBatch by warehouse+product+bin+batchNumber
                StockBatch stockBatch = stockBatchRepository
                        .findByWarehouse_WarehouseIdAndProduct_ProductIdAndBin_BinIdAndBatchNumber(
                                warehouse.getWarehouseId(),
                                product.getProductId(),
                                ginDetail.getBin().getBinId(),
                                ginDetail.getBatchNumber())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Không tìm thấy lô hàng cho sản phẩm '" + product.getProductName()
                                + "' tại bin " + ginDetail.getBin().getBinLocation() + "."));

                // Issue: decrease BOTH availableQty and reservedQty
                stockBatch.setQtyAvailable(stockBatch.getQtyAvailable() - baseQty);
                stockBatch.setQtyReserved(stockBatch.getQtyReserved() - baseQty);
                stockBatchRepository.save(stockBatch);

                // Log StockMovement: Issue / Physical
                StockMovement movement = StockMovement.builder()
                        .warehouse(warehouse)
                        .product(product)
                        .bin(ginDetail.getBin())
                        .batchNumber(ginDetail.getBatchNumber())
                        .movementType("Issue")
                        .stockType("Physical")
                        .quantity(baseQty)
                        .uom(product.getBaseUoM())
                        .balanceAfter(stockBatch.getQtyAvailable())
                        .build();
                stockMovementRepository.save(movement);

                // Update SO detail issuedQty (cumulative across all GINs)
                soDetail.setIssuedQty(soDetail.getIssuedQty() + inputIssuedQty);
            }
        }

        // Prevent posting with all issuedQty = 0 (infinite back-order loop)
        if (totalIssuedInput == 0) {
            throw new IllegalArgumentException("Vui lòng nhập số lượng thực xuất cho ít nhất một mặt hàng.");
        }

        // GIN → Posted
        gin.setGiStatus("Posted");
        ginRepository.save(gin);

        // Check SO completion — compare all SO details
        boolean allComplete = true;
        for (SalesOrderDetail soDetail : so.getDetails()) {
            if (soDetail.getIssuedQty() < soDetail.getOrderedQty()) {
                allComplete = false;
            }
        }

        String resultMessage;
        if (allComplete) {
            so.setSoStatus("Completed");
            soRepository.save(so);
            resultMessage = "Phiếu GIN " + gin.getGinNumber() + " đã ghi sổ thành công. Đơn hàng " + so.getSoNumber() + " đã hoàn thành.";
        } else {
            so.setSoStatus("Incomplete");
            soRepository.save(so);

            // Back-order: create new Draft GIN for remaining quantities
            String backOrderGinNumber = createBackOrderGIN(so, gin);
            resultMessage = "Phiếu GIN " + gin.getGinNumber() + " đã ghi sổ. Đơn hàng " + so.getSoNumber()
                    + " chưa xuất đủ. Phiếu bù " + backOrderGinNumber + " đã được tạo tự động.";
        }

        return resultMessage;
    }

    // ==================== Back-order Logic ====================

    private String createBackOrderGIN(SalesOrder so, GoodsIssueNote postedGin) {
        String ginNumber = generateGINNumber();
        GoodsIssueNote newGin = new GoodsIssueNote();
        newGin.setGinNumber(ginNumber);
        newGin.setSalesOrder(so);
        newGin.setWarehouse(so.getWarehouse());
        newGin.setGiStatus("Draft");
        newGin.setDetails(new ArrayList<>());

        for (SalesOrderDetail soDetail : so.getDetails()) {
            int remainingQty = soDetail.getOrderedQty() - soDetail.getIssuedQty();
            if (remainingQty <= 0) continue;

            // Find original GIN detail to reuse batch/bin info
            GoodsIssueDetail originalDetail = findOriginalGINDetail(postedGin, soDetail);

            GoodsIssueDetail newDetail = new GoodsIssueDetail();
            newDetail.setGoodsIssueNote(newGin);
            newDetail.setSalesOrderDetail(soDetail);
            newDetail.setProduct(soDetail.getProduct());
            newDetail.setIssuedQty(0); // Storekeeper fills in
            newDetail.setUom(soDetail.getUom());
            newDetail.setBatchNumber(originalDetail != null ? originalDetail.getBatchNumber() : "");
            newDetail.setBin(originalDetail != null ? originalDetail.getBin() : null);

            newGin.getDetails().add(newDetail);
        }

        ginRepository.save(newGin);
        return ginNumber;
    }

    private GoodsIssueDetail findOriginalGINDetail(GoodsIssueNote gin, SalesOrderDetail soDetail) {
        if (gin.getDetails() == null) return null;
        for (GoodsIssueDetail d : gin.getDetails()) {
            if (d.getSalesOrderDetail().getSoDetailId().equals(soDetail.getSoDetailId())) {
                return d;
            }
        }
        return null;
    }

    // ==================== GIN Number Generation ====================

    private String generateGINNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "GIN-" + dateStr + "-";
        String maxNumber = ginRepository.findMaxGinNumber(prefix);
        int nextNum = 1;
        if (maxNumber != null) {
            String suffix = maxNumber.substring(prefix.length());
            nextNum = Integer.parseInt(suffix) + 1;
        }
        return prefix + String.format("%03d", nextNum);
    }

    // ==================== UoM Conversion ====================

    private BigDecimal getConversionFactor(Product product, String uom) {
        if (uom.equals(product.getBaseUoM())) {
            return BigDecimal.ONE;
        }
        List<ProductUoMConversion> conversions = uomConversionRepository.findByProduct_ProductId(product.getProductId());
        for (ProductUoMConversion conv : conversions) {
            if (conv.getFromUoM().equals(uom)) {
                return BigDecimal.valueOf(conv.getConversionFactor());
            }
        }
        return BigDecimal.ONE;
    }

    // ==================== Mapping ====================

    private GoodsIssueNoteDTO mapToListDTO(GoodsIssueNote gin) {
        SalesOrder so = gin.getSalesOrder();
        return GoodsIssueNoteDTO.builder()
                .ginId(gin.getGinId())
                .ginNumber(gin.getGinNumber())
                .soNumber(so.getSoNumber())
                .customerName(so.getCustomer() != null ? so.getCustomer().getPartnerName() : null)
                .warehouseName(gin.getWarehouse() != null ? gin.getWarehouse().getWarehouseName() : null)
                .giStatus(gin.getGiStatus())
                .build();
    }

    private GoodsIssueNoteDTO mapToFullDTO(GoodsIssueNote gin) {
        GoodsIssueNoteDTO dto = mapToListDTO(gin);
        if (gin.getDetails() != null) {
            List<GoodsIssueDetailDTO> detailDTOs = gin.getDetails().stream()
                    .map(this::mapDetailToDTO)
                    .collect(Collectors.toList());
            dto.setDetails(detailDTOs);
        }
        return dto;
    }

    private GoodsIssueDetailDTO mapDetailToDTO(GoodsIssueDetail detail) {
        SalesOrderDetail soDetail = detail.getSalesOrderDetail();
        String displayName = "";
        if (detail.getProduct() != null) {
            displayName = detail.getProduct().getSku() + " - " + detail.getProduct().getProductName();
        }
        return GoodsIssueDetailDTO.builder()
                .giDetailId(detail.getGiDetailId())
                .productDisplayName(displayName)
                .uom(detail.getUom())
                .orderedQty(soDetail != null ? soDetail.getOrderedQty() - soDetail.getIssuedQty() : null)
                .issuedQty(detail.getIssuedQty())
                .batchNumber(detail.getBatchNumber())
                .binLocation(detail.getBin() != null ? detail.getBin().getBinLocation() : null)
                .build();
    }
}
