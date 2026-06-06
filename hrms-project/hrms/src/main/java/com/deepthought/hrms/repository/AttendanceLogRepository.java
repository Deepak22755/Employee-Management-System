package com.deepthought.hrms.repository;

import com.deepthought.hrms.entity.AttendanceLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, Long> {

        boolean existsByWorkerIdAndClockOutIsNull(Long workerId);

        Optional<AttendanceLog> findByWorkerIdAndClockOutIsNull(Long workerId);

        @EntityGraph(attributePaths = { "worker", "site" })
        @Query("SELECT a FROM AttendanceLog a " +
                        "WHERE a.worker.id = :workerId " +
                        "AND a.clockIn >= :from " +
                        "AND a.clockIn <= :to " +
                        "ORDER BY a.clockIn DESC")
        Page<AttendanceLog> findByWorkerIdAndDateRange(
                        @Param("workerId") Long workerId,
                        @Param("from") OffsetDateTime from,
                        @Param("to") OffsetDateTime to,
                        Pageable pageable);

        @Query("SELECT COALESCE(SUM(a.overtimeHours), 0) FROM AttendanceLog a " +
                        "WHERE a.worker.id = :workerId " +
                        "AND YEAR(a.clockIn) = :year " +
                        "AND MONTH(a.clockIn) = :month " +
                        "AND a.clockOut IS NOT NULL")
        java.math.BigDecimal sumOvertimeHoursForMonth(
                        @Param("workerId") Long workerId,
                        @Param("year") int year,
                        @Param("month") int month);
}
