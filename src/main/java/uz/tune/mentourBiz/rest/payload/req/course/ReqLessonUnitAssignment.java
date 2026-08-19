package uz.tune.mentourBiz.rest.payload.req.course;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ReqLessonUnitAssignment {
    private UUID lessonId;
    private List<UUID> unitUuids;
}
