package com.minhhai.wms.service.impl;

import com.minhhai.wms.dao.PartnerDao;
import com.minhhai.wms.entity.Partner;
import com.minhhai.wms.service.PartnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PartnerServiceImpl implements PartnerService {

    private final PartnerDao partnerDao;

    @Override
    @Transactional(readOnly = true)
    public List<Partner> findAll() {
        return partnerDao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Partner> findByType(String partnerType) {
        if (partnerType == null || partnerType.isBlank()) {
            return findAll();
        }
        return partnerDao.findByPartnerType(partnerType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Partner> search(String keyword, String partnerType) {
        if (keyword == null || keyword.isBlank()) {
            return findByType(partnerType);
        }
        if (partnerType != null && !partnerType.isBlank()) {
            return partnerDao.searchByNameAndType(keyword.trim(), partnerType);
        }
        return partnerDao.searchByName(keyword.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Partner> findById(Integer id) {
        return partnerDao.findById(id);
    }

    @Override
    public Partner save(Partner partner) {
        return partnerDao.save(partner);
    }

    @Override
    public Partner save(com.minhhai.wms.dto.PartnerDTO partnerDTO) {
        Partner partner;
        if (partnerDTO.getPartnerId() != null) {
            partner = partnerDao.findById(partnerDTO.getPartnerId())
                    .orElseThrow(() -> new IllegalArgumentException("Partner not found: " + partnerDTO.getPartnerId()));
            partner.setPartnerName(partnerDTO.getPartnerName());
            partner.setPartnerType(partnerDTO.getPartnerType());
            partner.setContactPerson(partnerDTO.getContactPerson());
            partner.setPhoneNumber(partnerDTO.getPhoneNumber());
        } else {
            partner = Partner.builder()
                    .partnerName(partnerDTO.getPartnerName())
                    .partnerType(partnerDTO.getPartnerType())
                    .contactPerson(partnerDTO.getContactPerson())
                    .phoneNumber(partnerDTO.getPhoneNumber())
                    .isActive(true)
                    .build();
        }

        return partnerDao.save(partner);
    }

    @Override
    public void toggleActive(Integer partnerId) {
        Partner partner = partnerDao.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Partner not found: " + partnerId));
        partner.setIsActive(!partner.getIsActive());
        partnerDao.save(partner);
    }
}
