package uz.tune.mentourBiz.rest.payload.studentRes.res.lesson;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ResLessonUnits {
    private UUID lessonUuid;
    private String lessonName;
    private Instant lessonDate;
    private String startTime;
    private String endTime;
    private List<ResLesson> units = new ArrayList<>();
}
