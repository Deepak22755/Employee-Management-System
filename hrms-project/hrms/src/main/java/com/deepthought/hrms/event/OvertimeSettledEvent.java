package com.deepthought.hrms.event;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class OvertimeSettledEvent {
    private final Long workerId;
    private final String workerName;
    private final String workerPhone;
    private final String month;
    private final BigDecimal totalAmount;
    private final int entriesSettled;

    public OvertimeSettledEvent(Long workerId, String workerName, String workerPhone,
            String month, BigDecimal totalAmount, int entriesSettled) {
        this.workerId = workerId;
        this.workerName = workerName;
        this.workerPhone = workerPhone;
        this.month = month;
        this.totalAmount = totalAmount;
        this.entriesSettled = entriesSettled;
    }
}
