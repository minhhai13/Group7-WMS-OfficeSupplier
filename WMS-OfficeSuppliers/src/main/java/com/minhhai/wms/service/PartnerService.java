package com.minhhai.wms.service;

import com.minhhai.wms.dto.PartnerDTO;
import com.minhhai.wms.entity.Partner;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface PartnerService {

    List<Partner> findAll();

    List<Partner> findByType(String partnerType);

    List<Partner> search(String keyword, String partnerType);

    Optional<Partner> findById(Integer id);

    Partner save(PartnerDTO partnerDTO);

    Partner save(Partner partner);

    void toggleActive(Integer partnerId);

    Page<Partner> findPaginated(String keyword, String partnerType, int page, int size);
}
