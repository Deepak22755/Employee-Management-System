package com.deepthought.hrms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SettlementResponse {
    private Long workerId;
    private String workerName;
    private String month;
    private int entriesSettled;
    private BigDecimal totalAmount;
    private String message;
}
