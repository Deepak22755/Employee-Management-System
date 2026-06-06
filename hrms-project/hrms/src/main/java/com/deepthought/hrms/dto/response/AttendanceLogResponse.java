package com.deepthought.hrms.dto.response;

import com.deepthought.hrms.enums.Designation;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
public class AttendanceLogResponse {
    private Long id;
    private Long workerId;
    private String workerName;
    private Designation designation;
    private Long siteId;
    private String siteName;
    private String siteLocation;
    private OffsetDateTime clockIn;
    private OffsetDateTime clockOut;
    private BigDecimal totalHours;
    private BigDecimal overtimeHours;
    private Boolean flagged;
}
