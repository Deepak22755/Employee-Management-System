package com.deepthought.hrms.service.impl;

import com.deepthought.hrms.dto.request.ClockInRequest;
import com.deepthought.hrms.dto.request.ClockOutRequest;
import com.deepthought.hrms.dto.response.ActiveWorkerResponse;
import com.deepthought.hrms.dto.response.AttendanceLogResponse;
import com.deepthought.hrms.dto.response.PagedResponse;
import com.deepthought.hrms.entity.AttendanceLog;
import com.deepthought.hrms.entity.OvertimeEntry;
import com.deepthought.hrms.entity.Site;
import com.deepthought.hrms.entity.Worker;
import com.deepthought.hrms.exception.BusinessRuleException;
import com.deepthought.hrms.exception.ResourceNotFoundException;
import com.deepthought.hrms.repository.AttendanceLogRepository;
import com.deepthought.hrms.repository.OvertimeEntryRepository;
import com.deepthought.hrms.repository.SiteRepository;
import com.deepthought.hrms.repository.WorkerRepository;
import com.deepthought.hrms.service.ActiveWorkerCacheService;
import com.deepthought.hrms.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceServiceImpl implements AttendanceService {

        private final WorkerRepository workerRepository;
        private final SiteRepository siteRepository;
        private final AttendanceLogRepository attendanceLogRepository;
        private final OvertimeEntryRepository overtimeEntryRepository;
        private final ActiveWorkerCacheService cacheService;

        @Value("${hrms.attendance.standard-shift-hours:8}")
        private int standardShiftHours;

        @Value("${hrms.attendance.max-shift-hours:16}")
        private int maxShiftHours;

        @Value("${hrms.attendance.monthly-overtime-cap-hours:60}")
        private int monthlyOvertimeCap;

        @Value("${hrms.overtime.tier1-hours:2}")
        private int tier1Hours;

        @Value("${hrms.overtime.tier1-multiplier:1.5}")
        private double tier1Multiplier;

        @Value("${hrms.overtime.tier2-multiplier:2.0}")
        private double tier2Multiplier;

        @Override
        @Transactional
        public AttendanceLogResponse clockIn(ClockInRequest request) {

                Worker worker = workerRepository.findByIdAndActiveTrue(request.getWorkerId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Worker not found or inactive: " + request.getWorkerId()));

                boolean alreadyClockedIn = cacheService.isWorkerActive(worker.getId())
                                || attendanceLogRepository.existsByWorkerIdAndClockOutIsNull(worker.getId());
                if (alreadyClockedIn) {
                        throw new BusinessRuleException("DUPLICATE_CLOCK_IN",
                                        "Worker is already clocked in. Clock out first.");
                }

                Site site = siteRepository.findByIdAndActiveTrue(request.getSiteId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Site not found or inactive: " + request.getSiteId()));

                OffsetDateTime now = OffsetDateTime.now();

                AttendanceLog log = AttendanceLog.builder()
                                .worker(worker)
                                .site(site)
                                .clockIn(now)
                                .build();

                AttendanceLog saved = attendanceLogRepository.save(log);

                cacheService.addActiveWorker(saved);

                return toResponse(saved);
        }

        @Override
        @Transactional
        public AttendanceLogResponse clockOut(ClockOutRequest request) {

                AttendanceLog log = attendanceLogRepository.findByWorkerIdAndClockOutIsNull(request.getWorkerId())
                                .orElseThrow(() -> new BusinessRuleException("NOT_CLOCKED_IN",
                                                "Worker is not currently clocked in"));

                OffsetDateTime clockOut = OffsetDateTime.now();
                log.setClockOut(clockOut);

                Duration duration = Duration.between(log.getClockIn(), clockOut);
                BigDecimal totalHours = BigDecimal.valueOf(duration.toMinutes())
                                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
                log.setTotalHours(totalHours);

                if (totalHours.compareTo(BigDecimal.valueOf(maxShiftHours)) > 0) {
                        log.setFlagged(true);
                        log.warn("Attendance {} flagged: total hours {} exceeds max shift {}",
                                        log.getId(), totalHours, maxShiftHours);
                }

                BigDecimal overtimeHours = BigDecimal.ZERO;
                if (totalHours.compareTo(BigDecimal.valueOf(standardShiftHours)) > 0) {
                        BigDecimal rawOvertime = totalHours.subtract(BigDecimal.valueOf(standardShiftHours));
                        overtimeHours = applyMonthlyOvertimeCap(log.getWorker().getId(), rawOvertime, clockOut);
                        log.setOvertimeHours(overtimeHours);

                        if (overtimeHours.compareTo(BigDecimal.ZERO) > 0) {
                                OvertimeEntry entry = calculateOvertimeEntry(log, overtimeHours);
                                overtimeEntryRepository.save(entry);
                        }
                }

                AttendanceLog saved = attendanceLogRepository.save(log);

                cacheService.removeActiveWorker(request.getWorkerId());

                return toResponse(saved);
        }

        @Override
        public List<ActiveWorkerResponse> getActiveWorkers() {
                return cacheService.getAllActiveWorkers();
        }

        @Override
        @Transactional(readOnly = true)
        public PagedResponse<AttendanceLogResponse> getAttendanceLog(
                        Long workerId, LocalDate from, LocalDate to, int page, int size) {

                workerRepository.findByIdAndActiveTrue(workerId)
                                .orElseThrow(() -> new ResourceNotFoundException("Worker not found: " + workerId));

                OffsetDateTime fromDt = from.atStartOfDay().atOffset(ZoneOffset.UTC);
                OffsetDateTime toDt = to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

                int safeSize = Math.min(size, 100);
                PageRequest pageRequest = PageRequest.of(page, safeSize, Sort.by("clockIn").descending());

                Page<AttendanceLog> pageResult = attendanceLogRepository
                                .findByWorkerIdAndDateRange(workerId, fromDt, toDt, pageRequest);

                return PagedResponse.of(pageResult.map(this::toResponse));
        }

        private BigDecimal applyMonthlyOvertimeCap(Long workerId, BigDecimal rawOvertime,
                        OffsetDateTime clockOut) {
                int year = clockOut.getYear();
                int month = clockOut.getMonthValue();

                BigDecimal alreadyAccruedThisMonth = overtimeEntryRepository
                                .sumOvertimeHoursForMonth(workerId, year, month);
                if (alreadyAccruedThisMonth == null)
                        alreadyAccruedThisMonth = BigDecimal.ZERO;

                BigDecimal remaining = BigDecimal.valueOf(monthlyOvertimeCap).subtract(alreadyAccruedThisMonth);

                if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                        log.info("Worker {} has hit monthly overtime cap for {}/{}", workerId, year, month);
                        return BigDecimal.ZERO;
                }

                return rawOvertime.min(remaining);
        }

        private OvertimeEntry calculateOvertimeEntry(AttendanceLog log, BigDecimal overtimeHours) {
                Worker worker = log.getWorker();
                BigDecimal hourlyWage = worker.getDailyWage()
                                .divide(BigDecimal.valueOf(standardShiftHours), 4, RoundingMode.HALF_UP);

                BigDecimal amount;
                if (overtimeHours.compareTo(BigDecimal.valueOf(tier1Hours)) <= 0) {

                        amount = overtimeHours.multiply(hourlyWage)
                                        .multiply(BigDecimal.valueOf(tier1Multiplier))
                                        .setScale(2, RoundingMode.HALF_UP);
                } else {

                        BigDecimal tier1Amount = BigDecimal.valueOf(tier1Hours)
                                        .multiply(hourlyWage)
                                        .multiply(BigDecimal.valueOf(tier1Multiplier));
                        BigDecimal tier2Hours = overtimeHours.subtract(BigDecimal.valueOf(tier1Hours));
                        BigDecimal tier2Amount = tier2Hours.multiply(hourlyWage)
                                        .multiply(BigDecimal.valueOf(tier2Multiplier));
                        amount = tier1Amount.add(tier2Amount).setScale(2, RoundingMode.HALF_UP);
                }

                BigDecimal effectiveRate = overtimeHours.compareTo(BigDecimal.ZERO) > 0
                                ? amount.divide(overtimeHours, 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;

                return OvertimeEntry.builder()
                                .worker(worker)
                                .attendanceLog(log)
                                .date(log.getClockIn().toLocalDate())
                                .overtimeHours(overtimeHours)
                                .overtimeRate(effectiveRate)
                                .amount(amount)
                                .build();
        }

        private AttendanceLogResponse toResponse(AttendanceLog log) {
                return AttendanceLogResponse.builder()
                                .id(log.getId())
                                .workerId(log.getWorker().getId())
                                .workerName(log.getWorker().getName())
                                .designation(log.getWorker().getDesignation())
                                .siteId(log.getSite().getId())
                                .siteName(log.getSite().getName())
                                .siteLocation(log.getSite().getLocation())
                                .clockIn(log.getClockIn())
                                .clockOut(log.getClockOut())
                                .totalHours(log.getTotalHours())
                                .overtimeHours(log.getOvertimeHours())
                                .flagged(log.getFlagged())
                                .build();
        }
}
