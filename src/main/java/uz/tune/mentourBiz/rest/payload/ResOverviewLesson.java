package uz.tune.mentourBiz.rest.payload;

import lombok.Data;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.enums.LessonStatus;
import uz.tune.mentourBiz.utils.DateUtils;

import java.time.Instant;
import java.util.UUID;

@Data
public class ResOverviewLesson {
    private UUID lessonId;
    private String lessonName;
    private String teacherName;
    private String groupName;
    private Integer studentCount;
    private Instant startTime;
    private Instant endTime;
    // School-local wall clock, computed server-side: "2026-08-14", "19:00", "20:30".
    private String date;
    private String startTimeLocal;
    private String endTimeLocal;
    private LessonStatus status;
    private String currentUnit;

    public ResOverviewLesson(UUID lessonId, String lessonName, String teacherName, String groupName,
                             Integer studentCount, Instant startTime, Instant endTime,
                             LessonStatus status, String currentUnit, School school) {
        this.lessonId = lessonId;
        this.lessonName = lessonName;
        this.teacherName = teacherName;
        this.groupName = groupName;
        this.studentCount = studentCount;
        this.startTime = startTime.plusSeconds(school.getUtcOffset() * 3600L);
        this.endTime = endTime.plusSeconds(school.getUtcOffset() * 3600L);
        this.date = DateUtils.schoolDate(startTime, school.getUtcOffset());
        this.startTimeLocal = DateUtils.schoolTime(startTime, school.getUtcOffset());
        this.endTimeLocal = DateUtils.schoolTime(endTime, school.getUtcOffset());
        this.status = LessonStatus.effective(status, endTime);
        this.currentUnit = currentUnit;
    }
}