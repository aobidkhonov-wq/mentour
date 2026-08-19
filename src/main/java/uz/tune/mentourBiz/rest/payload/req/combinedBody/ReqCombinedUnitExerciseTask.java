package uz.tune.mentourBiz.rest.payload.req.combinedBody;

import lombok.Data;
import uz.tune.mentourBiz.rest.enums.LessonSectionType;
import uz.tune.mentourBiz.rest.enums.UnitStatus;

import java.util.List;
import java.util.UUID;

@Data
public class ReqCombinedUnitExerciseTask {
    ReqCombinedUnits reqCombinedUnits;
    List<ReqCombinedTask> reqCreateTasks;

    @Data
    public static class ReqCombinedUnits {
        private UUID bookUuid;
        private String title;
        private String topic;
        private Integer sortOrder;
        private UnitStatus status;
    }

    @Data
    public static class ReqCombinedTask {
        private String title;
        private String topic;
        private Integer sortOrder;
        private LessonSectionType sectionType;
    }

}
