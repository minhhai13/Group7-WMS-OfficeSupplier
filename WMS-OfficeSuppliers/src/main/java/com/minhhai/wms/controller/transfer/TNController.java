package com.minhhai.wms.controller.transfer;

import com.minhhai.wms.dto.ProductDTO;
import com.minhhai.wms.dto.TransferNoteDTO;
import com.minhhai.wms.entity.Bin;
import com.minhhai.wms.entity.User;
import com.minhhai.wms.service.BinService;
import com.minhhai.wms.service.ProductService;
import com.minhhai.wms.service.TransferNoteService;
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
@RequestMapping("/transfer/internal")
@RequiredArgsConstructor
public class TNController {

    private final TransferNoteService transferNoteService;
    private final ProductService productService;
    private final BinService binService;

    @GetMapping
    public String list(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        Integer warehouseId = user.getWarehouse().getWarehouseId();
        List<TransferNoteDTO> transfers = transferNoteService.getTransferNotes(warehouseId);

        model.addAttribute("transfers", transfers);
        model.addAttribute("activePage", "transfer-internal");

        return "transfer/tn-list";
    }

    @GetMapping("/{id}/view")
    public String view(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        try {
            TransferNoteDTO dto = transferNoteService.getTransferNoteById(id);
            model.addAttribute("tn", dto);
            model.addAttribute("activePage", "transfer-internal");
            return "transfer/tn-detail"; // Màn hình xem chi tiết
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/transfer/internal";
        }
    }

    @GetMapping("/new")
    public String createForm(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        TransferNoteDTO dto = new TransferNoteDTO();
        dto.setWarehouseId(user.getWarehouse().getWarehouseId());
        dto.setWarehouseName(user.getWarehouse().getWarehouseName());

        List<Bin> bins = binService.findByWarehouseId(user.getWarehouse().getWarehouseId());

        model.addAttribute("tnDTO", dto);
        model.addAttribute("products", getActiveProductDTOs());
        model.addAttribute("bins", bins);
        model.addAttribute("activePage", "transfer-internal");

        return "transfer/tn-form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("tnDTO") TransferNoteDTO tnDTO,
                       BindingResult bindingResult,
                       Model model,
                       HttpSession session,
                       RedirectAttributes redirectAttributes) {

        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        if (bindingResult.hasErrors()) {
            List<Bin> bins = binService.findByWarehouseId(user.getWarehouse().getWarehouseId());
            model.addAttribute("products", getActiveProductDTOs());
            model.addAttribute("bins", bins);
            model.addAttribute("activePage", "transfer-internal");
            model.addAttribute("error", "Vui lòng sửa các lỗi bên dưới.");
            return "transfer/tn-form";
        }

        try {
            transferNoteService.createTransferNote(tnDTO, user);
            redirectAttributes.addFlashAttribute("success", "Phiếu chuyển kho nội bộ đã được tạo và ghi sổ thành công.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/transfer/internal/new";
        }

        return "redirect:/transfer/internal";
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        User user = (User) session.getAttribute("loggedInUser");
        try {
            transferNoteService.completeTransferNote(id, user);
            ra.addFlashAttribute("success", "Đã xác nhận chuyển bin và cập nhật kho thành công.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/transfer/internal";
    }

    private List<ProductDTO> getActiveProductDTOs() {
        return productService.findAllActive().stream()
                .map(p -> ProductDTO.builder()
                        .productId(p.getProductId())
                        .sku(p.getSku())
                        .productName(p.getProductName())
                        .baseUoM(p.getBaseUoM())
                        .build())
                .toList();
    }
}
