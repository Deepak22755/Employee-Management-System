package com.deepthought.hrms.service;

import com.deepthought.hrms.dto.response.OvertimeSummaryResponse;
import com.deepthought.hrms.dto.response.SettlementResponse;

public interface OvertimeService {
    OvertimeSummaryResponse getOvertimeSummary(Long workerId, String month);

    SettlementResponse settleOvertime(Long workerId, String month);
}
