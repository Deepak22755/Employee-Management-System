package com.deepthought.hrms.service;

import com.deepthought.hrms.dto.request.ClockInRequest;
import com.deepthought.hrms.dto.request.ClockOutRequest;
import com.deepthought.hrms.dto.response.ActiveWorkerResponse;
import com.deepthought.hrms.dto.response.AttendanceLogResponse;
import com.deepthought.hrms.dto.response.PagedResponse;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {
    AttendanceLogResponse clockIn(ClockInRequest request);

    AttendanceLogResponse clockOut(ClockOutRequest request);

    List<ActiveWorkerResponse> getActiveWorkers();

    PagedResponse<AttendanceLogResponse> getAttendanceLog(Long workerId, LocalDate from, LocalDate to, int page,
            int size);
}
