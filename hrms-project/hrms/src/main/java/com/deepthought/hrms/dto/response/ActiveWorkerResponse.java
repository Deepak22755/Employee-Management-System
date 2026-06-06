package com.deepthought.hrms.dto.response;

import com.deepthought.hrms.enums.Designation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveWorkerResponse implements Serializable {
    private Long workerId;
    private String workerName;
    private Designation designation;
    private Long siteId;
    private String siteName;
    private OffsetDateTime clockInTime;
}
