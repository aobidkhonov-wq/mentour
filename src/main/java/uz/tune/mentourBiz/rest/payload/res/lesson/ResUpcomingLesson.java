package uz.tune.mentourBiz.rest.payload.res.lesson;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.CourseLesson;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.LessonStatus;
import uz.tune.mentourBiz.rest.enums.LessonType;
import uz.tune.mentourBiz.rest.enums.UnitType;
import uz.tune.mentourBiz.utils.DateUtils;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
public class ResUpcomingLesson {

    private UUID lessonId;
    private String courseName;
    private String lessonName;
    private String schoolName;
    private String description;
    private Instant startTime;
    private Instant endTime;
    // School-local wall clock, computed server-side: "2026-08-14", "19:00", "20:30".
    private String date;
    private String startTimeLocal;
    private String endTimeLocal;
    private String joinLink;
    private LessonStatus status;
    private LessonType type;
    private boolean hasExam;

    public ResUpcomingLesson(CourseLesson lesson, User currentUser) {
        this.lessonId = lesson.getUuid();
        this.lessonName = lesson.getName();
        this.courseName = lesson.getCourse().getName();
        this.schoolName = lesson.getCourse().getSchool().getName();
        this.startTime = lesson.getStartTime();
        this.endTime = lesson.getEndTime();
        Integer utcOffset = lesson.getCourse().getSchool() != null
                ? lesson.getCourse().getSchool().getUtcOffset() : null;
        this.date = DateUtils.schoolDate(lesson.getStartTime(), utcOffset);
        this.startTimeLocal = DateUtils.schoolTime(lesson.getStartTime(), utcOffset);
        this.endTimeLocal = DateUtils.schoolTime(lesson.getEndTime(), utcOffset);
        this.type = lesson.getLessonType();
        LessonStatus rawStatus = (this.type == LessonType.APP) ? LessonStatus.STUDENT_APP : lesson.getStatus();
        this.status = LessonStatus.effective(rawStatus, lesson.getEndTime());

        if (lesson.getUnits() != null && !lesson.getUnits().isEmpty()) {
            this.hasExam = lesson.getUnits().stream().anyMatch(u -> u.getUnitType() == UnitType.EXAM);
            this.description = "Covering: " + lesson.getUnits().stream()
                    .map(Unit::getTitle)
                    .collect(Collectors.joining(" | "));
        }
    }
}