package com.minhhai.wms.dao;

import com.minhhai.wms.entity.Partner;

import java.util.List;
import java.util.Optional;

public interface PartnerDao {

    List<Partner> findAll();

    List<Partner> findByPartnerType(String partnerType);

    List<Partner> findByIsActive(Boolean isActive);

    List<Partner> findByPartnerTypeAndIsActive(String partnerType, Boolean isActive);

    List<Partner> searchByName(String keyword);

    List<Partner> searchByNameAndType(String keyword, String type);

    Optional<Partner> findById(Integer id);

    Partner save(Partner partner);
}
