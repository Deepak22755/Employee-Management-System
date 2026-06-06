package com.deepthought.hrms.entity;

import com.deepthought.hrms.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "overtime_entries", indexes = {
                @Index(name = "idx_overtime_worker_month", columnList = "worker_id, date"),
                @Index(name = "idx_overtime_status", columnList = "settlement_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OvertimeEntry {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "worker_id", nullable = false)
        private Worker worker;

        @OneToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "attendance_id", nullable = false, unique = true)
        private AttendanceLog attendanceLog;

        @Column(nullable = false)
        private LocalDate date;

        @Column(name = "overtime_hours", nullable = false, precision = 5, scale = 2)
        private BigDecimal overtimeHours;

        @Column(name = "overtime_rate", nullable = false, precision = 10, scale = 2)
        private BigDecimal overtimeRate;

        @Column(nullable = false, precision = 10, scale = 2)
        private BigDecimal amount;

        @Enumerated(EnumType.STRING)
        @Column(name = "settlement_status", nullable = false, columnDefinition = "settlement_status")
        @Builder.Default
        private SettlementStatus settlementStatus = SettlementStatus.PENDING;

        @Column(name = "settled_at")
        private OffsetDateTime settledAt;

        @CreationTimestamp
        @Column(name = "created_at", updatable = false)
        private OffsetDateTime createdAt;
}
