package com.deepthought.hrms.controller;

import com.deepthought.hrms.entity.Site;
import com.deepthought.hrms.exception.ResourceNotFoundException;
import com.deepthought.hrms.repository.SiteRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sites")
@RequiredArgsConstructor
public class SiteController {

    private final SiteRepository siteRepository;

    @PostMapping
    @Transactional
    public ResponseEntity<Site> createSite(@Valid @RequestBody CreateSiteRequest req) {
        Site site = Site.builder()
                .name(req.getName())
                .location(req.getLocation())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(siteRepository.save(site));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Site> getSite(@PathVariable Long id) {
        return siteRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Site not found: " + id));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deactivateSite(@PathVariable Long id) {
        Site site = siteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Site not found: " + id));
        site.setActive(false);
        siteRepository.save(site);
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class CreateSiteRequest {
        @NotBlank
        private String name;
        @NotBlank
        private String location;
    }
}
