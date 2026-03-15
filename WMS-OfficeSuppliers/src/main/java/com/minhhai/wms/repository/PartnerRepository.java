package com.minhhai.wms.repository;

import com.minhhai.wms.entity.Partner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartnerRepository extends JpaRepository<Partner, Integer> {

    List<Partner> findByPartnerType(String partnerType);

    List<Partner> findByIsActive(Boolean isActive);

    List<Partner> findByPartnerTypeAndIsActive(String partnerType, Boolean isActive);

    @Query("SELECT p FROM Partner p WHERE LOWER(p.partnerName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Partner> searchByName(@Param("keyword") String keyword);

    @Query("SELECT p FROM Partner p WHERE p.partnerType = :type " +
            "AND LOWER(p.partnerName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Partner> searchByNameAndType(@Param("keyword") String keyword, @Param("type") String type);

    @Query("SELECT p FROM Partner p WHERE LOWER(p.partnerName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Partner> searchByNamePageable(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Partner p WHERE p.partnerType = :type " +
            "AND LOWER(p.partnerName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Partner> searchByNameAndTypePageable(@Param("keyword") String keyword,
                                              @Param("type") String type,
                                              Pageable pageable);

    Page<Partner> findByPartnerType(String type, Pageable pageable);
}
