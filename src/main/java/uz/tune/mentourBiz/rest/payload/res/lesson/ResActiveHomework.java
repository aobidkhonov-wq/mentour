package uz.tune.mentourBiz.rest.payload.res.lesson;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.payload.studentRes.res.lesson.ResLessonSection;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ResActiveHomework {
    private UUID unitUuid;
    private String unitTitle;
    private Instant dueDate;
    private List<ResLessonSection> sections;
    private Integer sortOrder;
    private boolean separateSection;
    private Integer globalRemainingSeconds;
    private boolean aiExplanationEnabled;
    private Integer minScoreToPass;
}
