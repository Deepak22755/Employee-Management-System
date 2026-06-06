package com.deepthought.hrms.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClockOutRequest {
    @NotNull(message = "workerId is required")
    private Long workerId;
}
