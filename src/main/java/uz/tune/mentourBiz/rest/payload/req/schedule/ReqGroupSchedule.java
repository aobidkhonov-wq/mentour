package uz.tune.mentourBiz.rest.payload.req.schedule;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class ReqGroupSchedule {
    private UUID groupUuid;
    private UUID lessonUuid;
    private Instant dueDate;
}