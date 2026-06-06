package com.deepthought.hrms.controller;

import com.deepthought.hrms.dto.request.ClockInRequest;
import com.deepthought.hrms.dto.request.ClockOutRequest;
import com.deepthought.hrms.dto.response.ActiveWorkerResponse;
import com.deepthought.hrms.dto.response.AttendanceLogResponse;
import com.deepthought.hrms.dto.response.PagedResponse;
import com.deepthought.hrms.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/clock-in")
    public ResponseEntity<AttendanceLogResponse> clockIn(@Valid @RequestBody ClockInRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attendanceService.clockIn(request));
    }

    @PostMapping("/clock-out")
    public ResponseEntity<AttendanceLogResponse> clockOut(@Valid @RequestBody ClockOutRequest request) {
        return ResponseEntity.ok(attendanceService.clockOut(request));
    }

    @GetMapping("/active")
    public ResponseEntity<List<ActiveWorkerResponse>> getActiveWorkers() {
        return ResponseEntity.ok(attendanceService.getActiveWorkers());
    }

    @GetMapping("/log")
    public ResponseEntity<PagedResponse<AttendanceLogResponse>> getAttendanceLog(
            @RequestParam Long workerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(attendanceService.getAttendanceLog(workerId, from, to, page, size));
    }
}
