package uz.tune.mentourBiz.rest.payload.res.course;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.CourseLesson;
import uz.tune.mentourBiz.rest.enums.AttendanceStatus;
import uz.tune.mentourBiz.rest.enums.LessonStatus;
import uz.tune.mentourBiz.utils.DateUtils;

import java.time.Instant;
import java.util.UUID;


@Getter
@Setter
public class ResLessonForCourseView {
    private UUID lessonId;
    private String name;
    private Instant startTime;
    private Instant endTime;
    // School-local wall clock, computed server-side: "2026-08-14", "19:00", "20:30".
    private String date;
    private String startTimeLocal;
    private String endTimeLocal;
    private Long durationMinutes;
    private String joinLink;
    private LessonStatus status;
    private AttendanceStatus attendanceStatus;

    public ResLessonForCourseView(CourseLesson lesson, String dynamicJoinLink, AttendanceStatus attendanceStatus,Integer utcOffset) {
        this.lessonId = lesson.getUuid();
        this.name = lesson.getName();
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
        this.status = LessonStatus.effective(lesson.getStatus(), lesson.getEndTime());
        this.attendanceStatus = attendanceStatus;

        if (lesson.getStartTime() != null && lesson.getEndTime() != null) {
            this.durationMinutes = java.time.Duration.between(lesson.getStartTime(), lesson.getEndTime()).toMinutes();
        } else {
            this.durationMinutes = 0L;
        }

        if (this.status.equals(LessonStatus.FINISHED)) {
            this.joinLink = null;
        } else {
            this.joinLink = dynamicJoinLink;
        }
    }

}