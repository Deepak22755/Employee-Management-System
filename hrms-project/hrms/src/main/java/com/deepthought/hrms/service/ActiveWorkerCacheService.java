package com.deepthought.hrms.service;

import com.deepthought.hrms.dto.response.ActiveWorkerResponse;
import com.deepthought.hrms.entity.AttendanceLog;

import java.util.List;

public interface ActiveWorkerCacheService {
    void addActiveWorker(AttendanceLog log);

    void removeActiveWorker(Long workerId);

    List<ActiveWorkerResponse> getAllActiveWorkers();

    void invalidateWorkerCache(Long workerId);

    boolean isWorkerActive(Long workerId);
}
