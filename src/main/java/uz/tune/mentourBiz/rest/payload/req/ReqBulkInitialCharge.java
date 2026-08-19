package uz.tune.mentourBiz.rest.payload.req;

import java.util.List;
import java.util.UUID;

public record ReqBulkInitialCharge(
        List<UUID> studentUuids,
        Long amount,
        String note
) {}