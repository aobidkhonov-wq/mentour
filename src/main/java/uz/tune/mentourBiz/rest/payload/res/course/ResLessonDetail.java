package uz.tune.mentourBiz.rest.payload.res.course;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.CourseLesson;
import uz.tune.mentourBiz.rest.enums.AttendanceStatus;
import uz.tune.mentourBiz.rest.enums.LessonStatus;
import uz.tune.mentourBiz.rest.enums.LessonType;
import uz.tune.mentourBiz.rest.enums.UnitStatus;
import uz.tune.mentourBiz.rest.payload.res.lesson.ResUnit;
import uz.tune.mentourBiz.utils.DateUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
public class ResLessonDetail {
    private UUID id;
    private String name;
    private String description;
    private LessonStatus status;
    private Instant startTime;
    private Instant endTime;
    // School-local wall clock, computed server-side: "2026-08-14", "19:00", "20:30".
    private String date;
    private String startTimeLocal;
    private String endTimeLocal;
//    private String link;
    private LessonType lessonType;
    private AttendanceStatus attendanceStatus;
    // units
    private List<ResUnit> units;

    public ResLessonDetail(CourseLesson lesson) {
        Integer utcOffset = lesson.getCourse().getGroup().getBranch().getSchool().getUtcOffset();
        this.id = lesson.getUuid();
        this.name = lesson.getName();
        this.status = LessonStatus.effective(lesson.getStatus(), lesson.getEndTime());
        this.description = lesson.getDescription();
        if (utcOffset != null) {
            this.startTime = lesson.getStartTime().plusSeconds(utcOffset * 3600L);
            this.endTime = lesson.getEndTime().plusSeconds(utcOffset * 3600L);
        } else {
            this.startTime = lesson.getStartTime();
            this.endTime = lesson.getEndTime();
        }
        this.date = DateUtils.schoolDate(lesson.getStartTime(), utcOffset);
        this.startTimeLocal = DateUtils.schoolTime(lesson.getStartTime(), utcOffset);
        this.endTimeLocal = DateUtils.schoolTime(lesson.getEndTime(), utcOffset);
        this.lessonType = lesson.getLessonType();
        if (lesson.getUnits() != null && !lesson.getUnits().isEmpty()) {
            this.units = lesson.getUnits().stream()
                    .filter(u -> u.getStatus() == UnitStatus.ACTIVE)
                    .map(ResUnit::new)
                    .collect(Collectors.toList());
        }
    }



}