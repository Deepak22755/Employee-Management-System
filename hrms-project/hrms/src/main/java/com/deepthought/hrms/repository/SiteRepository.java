package com.deepthought.hrms.repository;

import com.deepthought.hrms.entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {
    Optional<Site> findByIdAndActiveTrue(Long id);
}
