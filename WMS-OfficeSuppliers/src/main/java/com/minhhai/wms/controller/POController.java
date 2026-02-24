package com.minhhai.wms.controller;

import com.minhhai.wms.dto.PurchaseOrderDTO;
import com.minhhai.wms.dto.PurchaseOrderDetailDTO;
import com.minhhai.wms.entity.Partner;
import com.minhhai.wms.entity.Product;
import com.minhhai.wms.entity.PurchaseOrder;
import com.minhhai.wms.entity.PurchaseOrderDetail;
import com.minhhai.wms.entity.User;
import com.minhhai.wms.repository.PartnerRepository;
import com.minhhai.wms.repository.ProductRepository;
import com.minhhai.wms.service.PurchaseOrderService;
import com.minhhai.wms.dto.ProductDTO;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/purchasing")
@RequiredArgsConstructor
public class POController {

    private final PurchaseOrderService poService;
    private final PartnerRepository partnerRepository;
    private final ProductRepository productRepository;

    @GetMapping("/po-list")
    public String listPOs(@RequestParam(name = "status", required = false) String status,
                          Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user.getWarehouse() == null) {
            model.addAttribute("error", "You do not have a warehouse assigned.");
            return "error/403";
        }
        
        List<PurchaseOrder> pos = poService.getPOsByWarehouse(user.getWarehouse().getWarehouseId(), status);
        model.addAttribute("pos", pos);
        model.addAttribute("activePage", "po-list");
        model.addAttribute("statusFilter", status);
        return "purchasing/po-list";
    }

    @GetMapping("/po-form")
    public String showPOForm(@RequestParam(name = "id", required = false) Integer poId,
                             Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        
        PurchaseOrderDTO poDTO = new PurchaseOrderDTO();
        if (poId != null) {
            PurchaseOrder po = poService.getPOById(poId);
            
            // Check if user belongs to the same warehouse
            if (!po.getWarehouse().getWarehouseId().equals(user.getWarehouse().getWarehouseId())) {
                return "redirect:/403";
            }
            
            poDTO.setPoId(po.getPoId());
            poDTO.setPoNumber(po.getPoNumber());
            poDTO.setSupplierId(po.getSupplier().getPartnerId());
            poDTO.setSupplierName(po.getSupplier().getPartnerName());
            poDTO.setPoStatus(po.getPoStatus());
            
            List<PurchaseOrderDetailDTO> details = new ArrayList<>();
            for (PurchaseOrderDetail detail : po.getDetails()) {
                PurchaseOrderDetailDTO detailDTO = new PurchaseOrderDetailDTO();
                detailDTO.setPoDetailId(detail.getPoDetailId());
                detailDTO.setProductId(detail.getProduct().getProductId());
                detailDTO.setProductSku(detail.getProduct().getSku());
                detailDTO.setProductName(detail.getProduct().getProductName());
                detailDTO.setOrderedQty(detail.getOrderedQty());
                detailDTO.setReceivedQty(detail.getReceivedQty());
                detailDTO.setUom(detail.getUom());
                details.add(detailDTO);
            }
            poDTO.setDetails(details);
        } else {
            poDTO.setPoStatus("Draft");
            // Add one empty detail row by default
            poDTO.getDetails().add(new PurchaseOrderDetailDTO());
        }

        List<Partner> suppliers = partnerRepository.findByPartnerTypeAndIsActive("Supplier", true);
        
        // Pass ProductDTOs instead of Entities to avoid infinite recursion in JSON serialization
        List<ProductDTO> products = productRepository.findByIsActive(true).stream()
            .map(p -> ProductDTO.builder()
                .productId(p.getProductId())
                .sku(p.getSku())
                .productName(p.getProductName())
                .build())
            .collect(Collectors.toList());
        
        model.addAttribute("poDTO", poDTO);
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("products", products);
        model.addAttribute("activePage", "po-form");
        return "purchasing/po-form";
    }

    @PostMapping("/po-save")
    public String savePO(@Valid @ModelAttribute("poDTO") PurchaseOrderDTO poDTO,
                         BindingResult bindingResult,
                         @RequestParam(name = "action") String action,
                         Model model, HttpSession session,
                         RedirectAttributes redirectAttributes) {
                         
        User user = (User) session.getAttribute("loggedInUser");
        Integer warehouseId = user.getWarehouse().getWarehouseId();

        if (bindingResult.hasErrors()) {
            List<Partner> suppliers = partnerRepository.findByPartnerTypeAndIsActive("Supplier", true);
            
            // Re-populate products as DTOs
            List<ProductDTO> products = productRepository.findByIsActive(true).stream()
                .map(p -> ProductDTO.builder()
                    .productId(p.getProductId())
                    .sku(p.getSku())
                    .productName(p.getProductName())
                    .build())
                .collect(Collectors.toList());
                    
            model.addAttribute("suppliers", suppliers);
            model.addAttribute("products", products);
            model.addAttribute("activePage", "po-form");
            // fill missing product details for view rendering
            for (PurchaseOrderDetailDTO detail : poDTO.getDetails()) {
                if (detail.getProductId() != null) {
                    productRepository.findById(detail.getProductId()).ifPresent(p -> {
                        detail.setProductSku(p.getSku());
                        detail.setProductName(p.getProductName());
                    });
                }
            }
            return "purchasing/po-form";
        }
        
        // Filter out empty rows if any
        poDTO.getDetails().removeIf(d -> d.getProductId() == null);
        
        if (poDTO.getDetails().isEmpty()) {
            bindingResult.rejectValue("details", "error.poDTO", "At least one product line is required");
            List<Partner> suppliers = partnerRepository.findByPartnerTypeAndIsActive("Supplier", true);
            
            // Re-populate products as DTOs
            List<ProductDTO> products = productRepository.findByIsActive(true).stream()
                .map(p -> ProductDTO.builder()
                    .productId(p.getProductId())
                    .sku(p.getSku())
                    .productName(p.getProductName())
                    .build())
                .collect(Collectors.toList());
                    
            model.addAttribute("suppliers", suppliers);
            model.addAttribute("products", products);
            model.addAttribute("activePage", "po-form");
            return "purchasing/po-form";
        }

        try {
            boolean isDraft = action.equals("draft");
            PurchaseOrder savedPo = poService.savePO(poDTO, warehouseId, isDraft);
            
            if (isDraft) {
                redirectAttributes.addFlashAttribute("successMessage", "Draft saved successfully.");
                return "redirect:/purchasing/po-form?id=" + savedPo.getPoId();
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Purchase Order submitted for approval.");
                return "redirect:/purchasing/po-list";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/purchasing/po-list";
        }
    }
    
    @PostMapping("/po-delete/{id}")
    public String deletePO(@PathVariable("id") Integer poId,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        try {
            poService.deletePO(poId, user.getWarehouse().getWarehouseId());
            redirectAttributes.addFlashAttribute("successMessage", "Purchase Order deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/purchasing/po-list";
    }
}
