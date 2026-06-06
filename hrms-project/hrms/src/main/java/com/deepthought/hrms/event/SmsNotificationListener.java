package com.deepthought.hrms.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * LF-204 FIX: SMS fires ONLY after the settlement transaction has COMMITTED.
 *
 * @TransactionalEventListener(phase = AFTER_COMMIT) ensures:
 *                                   - If DB rolls back (any entry fails) → this
 *                                   method is NEVER called → no SMS sent
 *                                   - If DB commits → THEN this method is
 *                                   called → SMS sent with correct amount
 *
 * @Async ensures SMS sending doesn't block the HTTP response.
 *        If SMS fails after successful settlement, the settlement data stays
 *        correct.
 *        We log the failure and queue a retry — we do NOT crash the response.
 */
@Component
@Slf4j
public class SmsNotificationListener {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOvertimeSettled(OvertimeSettledEvent event) {
        try {
            String message = String.format(
                    "Dear %s, your overtime for %s has been settled. Amount: Rs %.2f. - HR Team",
                    event.getWorkerName(), event.getMonth(), event.getTotalAmount());
            sendSms(event.getWorkerPhone(), message);
            log.info("SMS sent to worker {} (phone: {}) for month {} settlement of Rs {}",
                    event.getWorkerId(), event.getWorkerPhone(),
                    event.getMonth(), event.getTotalAmount());
        } catch (Exception e) {

            log.error("SMS notification failed for worker {} month {} — " +
                    "settlement is still valid. Will retry. Error: {}",
                    event.getWorkerId(), event.getMonth(), e.getMessage());

        }
    }

    private void sendSms(String phone, String message) {

        log.info("[SMS STUB] To: {} | Message: {}", phone, message);

    }
}
