package uz.tune.mentourBiz.rest.payload.studentRes.res.lesson;


import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.enums.OverallStatus;
import uz.tune.mentourBiz.rest.enums.UnitType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class ResLesson {
    private UUID unitUuid;
    private String unitTitle;
    private Integer unitSortOrder;
    private Instant dueDate;
    private String topicName;
    private OverallStatus overallStatus;
    private Integer progressPercentage;
    private boolean isAdditional;
    private List<ResLessonSection> sections = new ArrayList<>();

    private UnitType unitType;
    private Map<String, Object> examPolicy;
}