package com.deepthought.hrms.repository;

import com.deepthought.hrms.entity.OvertimeEntry;
import com.deepthought.hrms.enums.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OvertimeEntryRepository extends JpaRepository<OvertimeEntry, Long> {

        @Query("SELECT o FROM OvertimeEntry o " +
                        "WHERE o.worker.id = :workerId " +
                        "AND YEAR(o.date) = :year " +
                        "AND MONTH(o.date) = :month " +
                        "ORDER BY o.date ASC")
        List<OvertimeEntry> findByWorkerIdAndMonth(
                        @Param("workerId") Long workerId,
                        @Param("year") int year,
                        @Param("month") int month);

        @Query("SELECT o FROM OvertimeEntry o " +
                        "WHERE o.worker.id = :workerId " +
                        "AND YEAR(o.date) = :year " +
                        "AND MONTH(o.date) = :month " +
                        "AND o.settlementStatus = 'PENDING'")
        List<OvertimeEntry> findPendingByWorkerIdAndMonth(
                        @Param("workerId") Long workerId,
                        @Param("year") int year,
                        @Param("month") int month);

        @Query("SELECT COALESCE(SUM(o.overtimeHours), 0) FROM OvertimeEntry o " +
                        "WHERE o.worker.id = :workerId " +
                        "AND YEAR(o.date) = :year " +
                        "AND MONTH(o.date) = :month")
        BigDecimal sumOvertimeHoursForMonth(
                        @Param("workerId") Long workerId,
                        @Param("year") int year,
                        @Param("month") int month);

        @Query("SELECT COALESCE(SUM(o.amount), 0) FROM OvertimeEntry o " +
                        "WHERE o.worker.id = :workerId " +
                        "AND YEAR(o.date) = :year " +
                        "AND MONTH(o.date) = :month " +
                        "AND o.settlementStatus = 'SETTLED'")
        BigDecimal sumSettledAmountForMonth(
                        @Param("workerId") Long workerId,
                        @Param("year") int year,
                        @Param("month") int month);

        Optional<OvertimeEntry> findByAttendanceLogId(Long attendanceId);

        @Modifying
        @Query("UPDATE OvertimeEntry o SET o.settlementStatus = :status, o.settledAt = :settledAt " +
                        "WHERE o.worker.id = :workerId " +
                        "AND YEAR(o.date) = :year " +
                        "AND MONTH(o.date) = :month " +
                        "AND o.settlementStatus = 'PENDING'")
        int settleAllForWorkerMonth(
                        @Param("workerId") Long workerId,
                        @Param("year") int year,
                        @Param("month") int month,
                        @Param("status") SettlementStatus status,
                        @Param("settledAt") OffsetDateTime settledAt);
}
