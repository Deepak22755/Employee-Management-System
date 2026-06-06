package com.deepthought.hrms.controller;

import com.deepthought.hrms.dto.response.OvertimeSummaryResponse;
import com.deepthought.hrms.dto.response.SettlementResponse;
import com.deepthought.hrms.service.OvertimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/overtime")
@RequiredArgsConstructor
public class OvertimeController {

    private final OvertimeService overtimeService;

    @GetMapping("/summary/{workerId}")
    public ResponseEntity<OvertimeSummaryResponse> getOvertimeSummary(
            @PathVariable Long workerId,
            @RequestParam String month) {
        return ResponseEntity.ok(overtimeService.getOvertimeSummary(workerId, month));
    }

    @PostMapping("/settle/{workerId}")
    public ResponseEntity<SettlementResponse> settleOvertime(
            @PathVariable Long workerId,
            @RequestParam String month) {
        return ResponseEntity.ok(overtimeService.settleOvertime(workerId, month));
    }
}
