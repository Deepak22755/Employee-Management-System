package com.deepthought.hrms.dto.response;

import com.deepthought.hrms.enums.SettlementStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class OvertimeSummaryResponse {
    private Long workerId;
    private String workerName;
    private String month;
    private BigDecimal totalOvertimeHours;
    private BigDecimal totalPayoutAmount;
    private SettlementStatus overallStatus;
    private List<OvertimeDayBreakdown> breakdown;

    @Data
    @Builder
    public static class OvertimeDayBreakdown {
        private String date;
        private BigDecimal overtimeHours;
        private BigDecimal amount;
        private SettlementStatus settlementStatus;
    }
}
