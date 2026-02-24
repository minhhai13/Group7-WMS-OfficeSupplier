package com.minhhai.wms.controller;

import com.minhhai.wms.dto.PartnerDTO;
import com.minhhai.wms.entity.Partner;
import com.minhhai.wms.service.PartnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/warehouse/partners")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerService partnerService;

    @GetMapping
    public String list(@RequestParam(name = "type", required = false) String type,
                       @RequestParam(name = "keyword", required = false) String keyword,
                       Model model) {
        List<Partner> partners = partnerService.search(keyword, type);
        model.addAttribute("activePage", "warehouse-partners");
        model.addAttribute("partners", partners);
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
