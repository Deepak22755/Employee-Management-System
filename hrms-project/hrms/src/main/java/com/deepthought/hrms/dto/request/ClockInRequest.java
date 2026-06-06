package com.deepthought.hrms.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClockInRequest {
    @NotNull(message = "workerId is required")
    private Long workerId;

    @NotNull(message = "siteId is required")
    private Long siteId;
}
