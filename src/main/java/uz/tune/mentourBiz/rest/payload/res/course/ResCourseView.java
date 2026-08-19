package uz.tune.mentourBiz.rest.payload.res.course;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.Course;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Teacher;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.AttendanceStatus;
import uz.tune.mentourBiz.rest.enums.LessonStatus;
import uz.tune.mentourBiz.rest.payload.res.ResBooks;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;
import uz.tune.mentourBiz.rest.payload.res.level.ResLevel;
import uz.tune.mentourBiz.utils.CoreUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


@Getter
@Setter
public class ResCourseView {

    private UUID id;
    private UUID schoolId;
    private UUID groupId;
    private UUID mentorId;
    private UUID teacherId;
    private List<ResBooks> books;
    private String courseName;
    private String schoolName;
    private String teacherName;
    private ResAttachment teacherAttachment;
    private String moderatorName;
    private Integer studentCount;
    private ResLevel level;

    private String description;
    private Integer numberOfLessons;
    private long courseDurationHours;
    private List<ResLessonForCourseView> lessons;
    private Double unitProgresses;
    private Integer lastLessonIndex;

    // Course duration soft limit
    private Instant courseStartDate;
    private Instant courseDueDate;
    private boolean overdue;
    private Integer daysOverdue;
    // How much of the course duration has elapsed so far (0-100, 100 when overdue);
    // null when neither the course nor the school settings define a duration.
    private Integer durationProgressPercent;

    public ResCourseView(Course course, long durationHours,
                         User currentUser,
                         Map<UUID, AttendanceStatus> attendanceMap, Integer studentCount, Double unitProgresses) {
        this.id = course.getUuid();
        this.schoolId = course.getSchool().getUuid();
        this.unitProgresses = unitProgresses;
        if(CoreUtils.isPresent(course.getGroup().getTeacher())) {
            if (course.getGroup().getTeacher().getUser() != null) {
                Teacher teacher = course.getGroup().getTeacher();
                this.teacherId = teacher.getUser().getUuid();
                this.moderatorName = teacher.getUser().getFirstName() + " " + teacher.getUser().getLastName();
                if(teacher.getUser().getAttachment() != null){
                    this.teacherAttachment = new ResAttachment(teacher.getUser().getAttachment());
                }
            }
        }
        this.groupId = course.getGroup().getUuid();
        if (course.getLinkedBooks() != null) {
            this.books = course.getLinkedBooks().stream()
                    .map(ResBooks::new)
                    .toList();
        }
        this.courseName = course.getName();
        this.schoolName = course.getSchool().getName();
        this.studentCount = studentCount;
        this.level = new ResLevel(course.getGroup().getLevel());
        this.description = course.getDescription();
        this.numberOfLessons = Math.toIntExact(course.getLessons().stream().filter(lessons -> !lessons.getStatus().equals(LessonStatus.DELETED)).count());
        this.courseDurationHours = durationHours;
        this.lessons = course.getLessons().stream()
                .filter(lesson -> !lesson.getStatus().equals(LessonStatus.DELETED))
                .map(lesson -> {

                    String dynamicJoinLink = null;
                    // bbb logic omitted for brevity as per existing code structure

                    AttendanceStatus attStatus = (attendanceMap != null)
                            ? attendanceMap.getOrDefault(lesson.getUuid(), AttendanceStatus.NOT_MARKED)
                            : null;

                    return new ResLessonForCourseView(lesson, dynamicJoinLink, attStatus,course.getSchool().getUtcOffset());
                })
                .collect(Collectors.toList());
    }

    public ResCourseView(Course course) {
        this.id = course.getUuid();
        this.courseName = course.getName();
        this.description = course.getDescription();

        if (course.getGroup() != null) {
            this.groupId = course.getGroup().getUuid();
        }

        if (course.getSchool() != null) {
            this.schoolId = course.getSchool().getUuid();
            this.schoolName = course.getSchool().getName();
        }

        if (course.getGroup() != null && course.getGroup().getTeacher() != null) {
            User teacherUser = course.getGroup().getTeacher().getUser();
            this.teacherId = teacherUser.getUuid();
            this.teacherName = teacherUser.getFirstName() + " " + teacherUser.getLastName();
        }
    }
}