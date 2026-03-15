package com.minhhai.wms.controller.admin;

import com.minhhai.wms.dto.PartnerDTO;
import com.minhhai.wms.entity.Partner;
import com.minhhai.wms.service.PartnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/warehouse/partners")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerService partnerService;

    private static final int PAGE_SIZE = 10;

    @GetMapping
    public String list(@RequestParam(name = "type", required = false) String type,
                       @RequestParam(name = "keyword", required = false) String keyword,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       Model model) {
        if (page < 0) page = 0;

        Page<Partner> partnerPage = partnerService.findPaginated(keyword, type, page, PAGE_SIZE);

        if (page > 0 && page >= partnerPage.getTotalPages()) {
            page = Math.max(0, partnerPage.getTotalPages() - 1);
            partnerPage = partnerService.findPaginated(keyword, type, page, PAGE_SIZE);
        }

        model.addAttribute("activePage", "warehouse-partners");
        model.addAttribute("partnerPage", partnerPage);
        model.addAttribute("partners", partnerPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", partnerPage.getTotalPages());
        model.addAttribute("totalElements", partnerPage.getTotalElements());
        model.addAttribute("selectedType", type);
        model.addAttribute("keyword", keyword);
        return "warehouse/partner-list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("activePage", "warehouse-partners");
        model.addAttribute("partner", new PartnerDTO());
        return "warehouse/partner-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable(name = "id") Integer id, Model model, RedirectAttributes redirectAttributes) {
        return partnerService.findById(id)
                .map(partner -> {
                    PartnerDTO dto = mapToDTO(partner);
                    model.addAttribute("activePage", "warehouse-partners");
                    model.addAttribute("partner", dto);
                    return "warehouse/partner-form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Partner not found.");
                    return "redirect:/warehouse/partners";
                });
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("partner") PartnerDTO partnerDTO,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("activePage", "warehouse-partners");
            return "warehouse/partner-form";
        }

        try {
            partnerService.save(partnerDTO);
            redirectAttributes.addFlashAttribute("success", "Partner saved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/warehouse/partners";
        }

        return "redirect:/warehouse/partners";
    }

    @PostMapping("/{id}/toggle")
    public String toggleActive(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes) {
        partnerService.toggleActive(id);
        redirectAttributes.addFlashAttribute("success", "Partner status updated.");
        return "redirect:/warehouse/partners";
    }

    private PartnerDTO mapToDTO(Partner partner) {
        return PartnerDTO.builder()
                .partnerId(partner.getPartnerId())
                .partnerName(partner.getPartnerName())
                .partnerType(partner.getPartnerType())
                .contactPerson(partner.getContactPerson())
                .phoneNumber(partner.getPhoneNumber())
                .isActive(partner.getIsActive())
                .build();
    }
}
