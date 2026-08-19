package uz.tune.mentourBiz.rest.payload.req;

import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
public class ReqRenewSub {
    private List<UUID> schoolUuids;
    private Instant date;
}
