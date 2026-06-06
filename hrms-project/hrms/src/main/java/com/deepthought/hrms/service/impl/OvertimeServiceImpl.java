package com.deepthought.hrms.service.impl;

import com.deepthought.hrms.dto.response.OvertimeSummaryResponse;
import com.deepthought.hrms.dto.response.SettlementResponse;
import com.deepthought.hrms.entity.OvertimeEntry;
import com.deepthought.hrms.entity.Worker;
import com.deepthought.hrms.enums.SettlementStatus;
import com.deepthought.hrms.event.OvertimeSettledEvent;
import com.deepthought.hrms.exception.BusinessRuleException;
import com.deepthought.hrms.exception.ResourceNotFoundException;
import com.deepthought.hrms.repository.OvertimeEntryRepository;
import com.deepthought.hrms.repository.WorkerRepository;
import com.deepthought.hrms.service.OvertimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OvertimeServiceImpl implements OvertimeService {

        private final WorkerRepository workerRepository;
        private final OvertimeEntryRepository overtimeEntryRepository;
        private final ApplicationEventPublisher eventPublisher;
        private final RestTemplate restTemplate;

        @Value("${hrms.external-api.min-wage-url:https://api.example.gov/min-wage}")
        private String minWageApiUrl;

        @Override
        public OvertimeSummaryResponse getOvertimeSummary(Long workerId, String month) {
                Worker worker = workerRepository.findByIdAndActiveTrue(workerId)
                                .orElseThrow(() -> new ResourceNotFoundException("Worker not found: " + workerId));

                YearMonth yearMonth = parseMonth(month);

                BigDecimal currentMinWage = fetchMinWageSafely();

                return buildSummary(worker, yearMonth, currentMinWage);
        }

        @Transactional(readOnly = true)
        protected OvertimeSummaryResponse buildSummary(Worker worker, YearMonth yearMonth,
                        BigDecimal minWage) {
                int year = yearMonth.getYear();
                int month = yearMonth.getMonthValue();

                List<OvertimeEntry> entries = overtimeEntryRepository
                                .findByWorkerIdAndMonth(worker.getId(), year, month);

                BigDecimal totalHours = entries.stream()
                                .map(OvertimeEntry::getOvertimeHours)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalAmount = entries.stream()
                                .map(OvertimeEntry::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                boolean anyPending = entries.stream()
                                .anyMatch(e -> e.getSettlementStatus() == SettlementStatus.PENDING);

                List<OvertimeSummaryResponse.OvertimeDayBreakdown> breakdown = entries.stream()
                                .map(e -> OvertimeSummaryResponse.OvertimeDayBreakdown.builder()
                                                .date(e.getDate().toString())
                                                .overtimeHours(e.getOvertimeHours())
                                                .amount(e.getAmount())
                                                .settlementStatus(e.getSettlementStatus())
                                                .build())
                                .collect(Collectors.toList());

                return OvertimeSummaryResponse.builder()
                                .workerId(worker.getId())
                                .workerName(worker.getName())
                                .month(yearMonth.toString())
                                .totalOvertimeHours(totalHours)
                                .totalPayoutAmount(totalAmount)
                                .overallStatus(anyPending ? SettlementStatus.PENDING : SettlementStatus.SETTLED)
                                .breakdown(breakdown)
                                .build();
        }

        @Override
        @Transactional
        public SettlementResponse settleOvertime(Long workerId, String month) {
                Worker worker = workerRepository.findByIdAndActiveTrue(workerId)
                                .orElseThrow(() -> new ResourceNotFoundException("Worker not found: " + workerId));

                YearMonth yearMonth = parseMonth(month);

                YearMonth currentMonth = YearMonth.now();
                if (!yearMonth.isBefore(currentMonth)) {
                        throw new BusinessRuleException("CANNOT_SETTLE_CURRENT_MONTH",
                                        "Cannot settle overtime for current or future month. Only past months can be settled.");
                }

                int year = yearMonth.getYear();
                int monthVal = yearMonth.getMonthValue();

                List<OvertimeEntry> pendingEntries = overtimeEntryRepository
                                .findPendingByWorkerIdAndMonth(worker.getId(), year, monthVal);

                if (pendingEntries.isEmpty()) {
                        throw new BusinessRuleException("NO_PENDING_ENTRIES",
                                        "No pending overtime entries found for worker " + workerId + " in " + month);
                }

                BigDecimal totalAmount = pendingEntries.stream()
                                .map(OvertimeEntry::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                OffsetDateTime now = OffsetDateTime.now();
                int settled = overtimeEntryRepository.settleAllForWorkerMonth(
                                worker.getId(), year, monthVal, SettlementStatus.SETTLED, now);

                log.info("Settled {} overtime entries for worker {} month {}, total amount: {}",
                                settled, workerId, month, totalAmount);

                eventPublisher.publishEvent(new OvertimeSettledEvent(
                                worker.getId(),
                                worker.getName(),
                                worker.getPhone(),
                                yearMonth.toString(),
                                totalAmount,
                                settled));

                return SettlementResponse.builder()
                                .workerId(worker.getId())
                                .workerName(worker.getName())
                                .month(yearMonth.toString())
                                .entriesSettled(settled)
                                .totalAmount(totalAmount)
                                .message(String.format("Successfully settled %d overtime entries totaling Rs %.2f",
                                                settled, totalAmount))
                                .build();
        }

        private YearMonth parseMonth(String month) {
                try {
                        return YearMonth.parse(month);
                } catch (Exception e) {
                        throw new BusinessRuleException("INVALID_MONTH_FORMAT",
                                        "Month must be in YYYY-MM format, got: " + month);
                }
        }

        private BigDecimal fetchMinWageSafely() {
                try {

                        BigDecimal minWage = restTemplate.getForObject(minWageApiUrl, BigDecimal.class);
                        return minWage != null ? minWage : BigDecimal.ZERO;
                } catch (Exception e) {
                        log.warn("Could not fetch minimum wage from external API: {}. Continuing without it.",
                                        e.getMessage());
                        return BigDecimal.ZERO;
                }
        }
}
