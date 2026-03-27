package com.minhhai.wms.controller.transfer;

import com.minhhai.wms.dto.ProductDTO;
import com.minhhai.wms.dto.TransferOrderDTO;
import com.minhhai.wms.dto.WarehouseDTO;
import com.minhhai.wms.entity.User;
import com.minhhai.wms.entity.Warehouse;
import com.minhhai.wms.service.ProductService;
import com.minhhai.wms.service.TransferOrderService;
import com.minhhai.wms.service.WarehouseService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/transfer/requests")
@RequiredArgsConstructor
public class TOController {
    private final TransferOrderService transferOrderService;
    private final WarehouseService warehouseService;
    private final ProductService productService;

    @GetMapping
    public String list(Model model,
                       @RequestParam(value = "status", required = false) String status,
                       @RequestParam(value = "sourceWarehouseId", required = false) Integer sourceWarehouseId,
                       HttpSession session){
        User user = (User) session.getAttribute("loggedInUser");
        if(user == null || user.getWarehouse() == null) return "redirect:/login";

        Integer warehouseId = user.getWarehouse().getWarehouseId();

        String filterStatus = "All".equals(status) ? null : status;

        List<TransferOrderDTO> transferOrders = transferOrderService.getOutgoingWHTransferOrders(warehouseId, sourceWarehouseId, filterStatus);
        List<Warehouse> sourceWarehouses = warehouseService.findAllActiveExcluding(warehouseId);
        model.addAttribute("transfers", transferOrders);
        model.addAttribute("sourceWarehouses", sourceWarehouses);
        model.addAttribute("activePage", "transfer-request");
        model.addAttribute("selectedSourceId",sourceWarehouseId);
        model.addAttribute("selectedStatus", status);
        return "transfer/to-list";
    }

    @GetMapping("/new")
    public String createForm(Model model,
                             HttpSession session){

        User user = (User) session.getAttribute("loggedInUser");
        if(user == null || user.getWarehouse() == null) return "redirect:/login";

        Integer destWarehouseId = user.getWarehouse().getWarehouseId();

        TransferOrderDTO dto = new TransferOrderDTO();
        dto.setDestinationWarehouseId(destWarehouseId);
        dto.setDestinationWarehouseName(user.getWarehouse().getWarehouseName());
        List<Warehouse> sourceWarehouses = warehouseService.findAllActiveExcluding(destWarehouseId);

        model.addAttribute("sourceWarehouses", sourceWarehouses);
        model.addAttribute("activePage", "transfer-request");
        model.addAttribute("trDTO", dto);
        model.addAttribute("products", getActiveProductDTOs());
        return "transfer/to-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(Model model,
                           HttpSession session,
                           @PathVariable Integer id,
                           RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if(user == null || user.getWarehouse() == null) return "redirect:/login";

        try{

            Integer destWarehouseId = user.getWarehouse().getWarehouseId();
            List<Warehouse> sourceWarehouses = warehouseService.findAllActiveExcluding(destWarehouseId);

            TransferOrderDTO dto = transferOrderService.getTOById(id);
            model.addAttribute("sourceWarehouses", sourceWarehouses);
            model.addAttribute("trDTO", dto);
            model.addAttribute("activePage", "transfer-request");
            model.addAttribute("products", getActiveProductDTOs());
            return "transfer/to-form";
        }catch(IllegalArgumentException e){
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/transfer/requests";
        }

    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("trDTO") TransferOrderDTO trDTO,
                       BindingResult bindingResult,
                       @RequestParam("action") String action,
                       Model model,
                       HttpSession session,
                       RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("loggedInUser");
        if(user == null || user.getWarehouse() == null) return "redirect:/login";

        //check vi pham cac dk cua cac field dto
        if(bindingResult.hasErrors()){
            trDTO.setDestinationWarehouseId(user.getWarehouse().getWarehouseId());
            trDTO.setDestinationWarehouseName(user.getWarehouse().getWarehouseName());

            Integer destWarehouseId = user.getWarehouse().getWarehouseId();
            List<Warehouse> sourceWarehouses = warehouseService.findAllActiveExcluding(destWarehouseId);

            model.addAttribute("sourceWarehouses", sourceWarehouses);
            model.addAttribute("products", getActiveProductDTOs());
            model.addAttribute("error", "Please fix the errors below.");
            model.addAttribute("activePage", "transfer-request");

            return "transfer/to-form";
        }
        //kiem tra la draft hay submit
        try{
            //để draft.equal action để tránh null
            if("draft".equals(action)){
                transferOrderService.saveDraftTO(trDTO, user);
                redirectAttributes.addFlashAttribute("success", "Transfer request saved as draft successfully.");
            }else{
                transferOrderService.submitTO(trDTO, user);
                redirectAttributes.addFlashAttribute("success", "Transfer request submitted successfully.");
            }
        }catch(IllegalArgumentException e){
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            if(trDTO.getToId() != null) return "redirect:/transfer/requests/" + trDTO.getToId() + "/edit";

            return "redirect:/transfer/requests/new";
        }
        return "redirect:/transfer/requests";
    }

    @PostMapping("/{id}/delete")
    public String delete(Model model,
                         HttpSession session,
                         @PathVariable Integer id,
                         RedirectAttributes redirectAttributes){
        User user = (User) session.getAttribute("loggedInUser");
        if(user == null || user.getWarehouse() == null) return "redirect:/login";
        try{
            transferOrderService.deleteTO(id, user);
            redirectAttributes.addFlashAttribute("success", "Transfer request deleted successfully.");
        }catch(IllegalArgumentException e){
            redirectAttributes.addFlashAttribute("error", e.getMessage());

        }
        return "redirect:/transfer/requests";
    }
    //helper
    private List<ProductDTO> getActiveProductDTOs() {
        return productService.findAllActive().stream()
                .map(p -> ProductDTO.builder()
                        .productId(p.getProductId())
                        .sku(p.getSku())
                        .productName(p.getProductName())
                        .build())
                .toList();
    }
}