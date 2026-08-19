package uz.tune.mentourBiz.rest.payload.res.school.group;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.CourseLesson;
import uz.tune.mentourBiz.utils.DateUtils;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ResNextLesson {

    private UUID lessonId;
    private String lessonName;
    private String unitTitle;
    private Instant startTime;
    private Instant endTime;
    // School-local wall clock, computed server-side: "2026-08-14", "19:00", "20:30".
    private String date;
    private String startTimeLocal;
    private String endTimeLocal;

    public ResNextLesson(CourseLesson lesson,int utcOffset) {
        this.lessonId = lesson.getUuid();
        this.lessonName = lesson.getName();
        this.startTime = lesson.getStartTime().plusSeconds(utcOffset * 3600L);
        this.endTime = lesson.getEndTime().plusSeconds(utcOffset * 3600L);
        this.date = DateUtils.schoolDate(lesson.getStartTime(), utcOffset);
        this.startTimeLocal = DateUtils.schoolTime(lesson.getStartTime(), utcOffset);
        this.endTimeLocal = DateUtils.schoolTime(lesson.getEndTime(), utcOffset);
        if (lesson.getUnits() != null && !lesson.getUnits().isEmpty()) {
            this.unitTitle = lesson.getUnits().get(0).getTitle();
        }
    }
}
