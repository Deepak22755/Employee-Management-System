package com.deepthought.hrms.controller;

import com.deepthought.hrms.entity.Worker;
import com.deepthought.hrms.enums.Designation;
import com.deepthought.hrms.exception.ResourceNotFoundException;
import com.deepthought.hrms.repository.WorkerRepository;
import com.deepthought.hrms.service.ActiveWorkerCacheService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/workers")
@RequiredArgsConstructor
public class WorkerController {

    private final WorkerRepository workerRepository;
    private final ActiveWorkerCacheService cacheService;

    @PostMapping
    @Transactional
    public ResponseEntity<Worker> createWorker(@Valid @RequestBody CreateWorkerRequest req) {
        Worker worker = Worker.builder()
                .name(req.getName())
                .phone(req.getPhone())
                .designation(req.getDesignation())
                .dailyWage(req.getDailyWage())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(workerRepository.save(worker));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Worker> getWorker(@PathVariable Long id) {
        return workerRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found: " + id));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Worker> updateWorker(@PathVariable Long id,
            @Valid @RequestBody CreateWorkerRequest req) {
        Worker worker = workerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found: " + id));

        worker.setName(req.getName());
        worker.setPhone(req.getPhone());
        worker.setDesignation(req.getDesignation());
        worker.setDailyWage(req.getDailyWage());

        Worker saved = workerRepository.save(worker);

        cacheService.invalidateWorkerCache(id);

        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deactivateWorker(@PathVariable Long id) {
        Worker worker = workerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Worker not found: " + id));
        worker.setActive(false);
        workerRepository.save(worker);
        cacheService.invalidateWorkerCache(id);
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class CreateWorkerRequest {
        @NotBlank
        private String name;
        @NotBlank
        private String phone;
        @NotNull
        private Designation designation;
        @NotNull
        @Positive
        private BigDecimal dailyWage;
    }
}
