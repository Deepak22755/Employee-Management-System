package com.deepthought.hrms.repository;

import com.deepthought.hrms.entity.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, Long> {
    Optional<Worker> findByIdAndActiveTrue(Long id);

    boolean existsByPhone(String phone);
}
