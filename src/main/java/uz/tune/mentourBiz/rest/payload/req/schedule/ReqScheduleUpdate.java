package uz.tune.mentourBiz.rest.payload.req.schedule;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class ReqScheduleUpdate {
    private UUID unitUuid;
    private UUID scheduleUuid;
    private Instant dueDate;
}
