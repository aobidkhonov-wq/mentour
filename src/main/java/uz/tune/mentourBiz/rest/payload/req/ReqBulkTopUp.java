package uz.tune.mentourBiz.rest.payload.req;

import uz.tune.mentourBiz.rest.enums.FinanceEnums;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReqBulkTopUp(
        List<UUID> studentUuids,
        Long amount,
        FinanceEnums.PaymentMethod method,
        String note,
        // Moment the payment is registered on, Tashkent local time (2026-08-05T17:37). Null -> now.
        LocalDateTime paymentDate
) {}