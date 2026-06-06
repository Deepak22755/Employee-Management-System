package com.deepthought.hrms.service.impl;

import com.deepthought.hrms.dto.response.ActiveWorkerResponse;
import com.deepthought.hrms.entity.AttendanceLog;
import com.deepthought.hrms.service.ActiveWorkerCacheService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActiveWorkerCacheServiceImpl implements ActiveWorkerCacheService {

    private static final String ACTIVE_WORKERS_KEY = "active_workers";
    private static final String TTL_KEY_PREFIX = "active_worker_ttl:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${hrms.attendance.active-worker-ttl-hours:16}")
    private long ttlHours;

    @Override
    public void addActiveWorker(AttendanceLog log) {
        try {
            ActiveWorkerResponse response = ActiveWorkerResponse.builder()
                    .workerId(log.getWorker().getId())
                    .workerName(log.getWorker().getName())
                    .designation(log.getWorker().getDesignation())
                    .siteId(log.getSite().getId())
                    .siteName(log.getSite().getName())
                    .clockInTime(log.getClockIn())
                    .build();

            String json = objectMapper.writeValueAsString(response);
            String workerKey = String.valueOf(log.getWorker().getId());

            redisTemplate.opsForHash().put(ACTIVE_WORKERS_KEY, workerKey, json);

            String ttlKey = TTL_KEY_PREFIX + workerKey;
            redisTemplate.opsForValue().set(ttlKey, "1", Duration.ofHours(ttlHours));

            log.info("Worker {} added to active workers cache", log.getWorker().getId());
        } catch (Exception e) {
            // LF-202: don't let Redis failure break clock-in
            log.warn("Failed to add worker {} to cache: {}", log.getWorker().getId(), e.getMessage());
        }
    }

    @Override
    public void removeActiveWorker(Long workerId) {
        try {
            redisTemplate.opsForHash().delete(ACTIVE_WORKERS_KEY, String.valueOf(workerId));
            redisTemplate.delete(TTL_KEY_PREFIX + workerId);
            log.info("Worker {} removed from active workers cache", workerId);
        } catch (Exception e) {
            log.warn("Failed to remove worker {} from cache: {}", workerId, e.getMessage());
        }
    }

    @Override
    public List<ActiveWorkerResponse> getAllActiveWorkers() {
        try {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(ACTIVE_WORKERS_KEY);
            List<ActiveWorkerResponse> result = new ArrayList<>();

            for (Map.Entry<Object, Object> entry : entries.entrySet()) {
                try {
                    String ttlKey = TTL_KEY_PREFIX + entry.getKey();
                    Boolean ttlExists = redisTemplate.hasKey(ttlKey);
                    if (Boolean.FALSE.equals(ttlExists)) {
                        log.warn("Stale active worker entry found for worker {}, removing", entry.getKey());
                        redisTemplate.opsForHash().delete(ACTIVE_WORKERS_KEY, entry.getKey());
                        continue;
                    }

                    ActiveWorkerResponse worker = objectMapper.readValue(
                            (String) entry.getValue(), ActiveWorkerResponse.class);
                    result.add(worker);
                } catch (JsonProcessingException e) {
                    log.error("Failed to deserialize active worker entry: {}", e.getMessage());
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to read active workers from cache: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public void invalidateWorkerCache(Long workerId) {

        removeActiveWorker(workerId);
        log.info("Cache invalidated for worker {} due to profile update", workerId);
    }

    @Override
    public boolean isWorkerActive(Long workerId) {
        try {
            return redisTemplate.opsForHash().hasKey(ACTIVE_WORKERS_KEY, String.valueOf(workerId));
        } catch (Exception e) {
            log.warn("Failed to check active status for worker {} in cache: {}", workerId, e.getMessage());
            return false;
        }
    }
}
