package uz.tune.mentourBiz.rest.payload.res.course;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.Course;
import uz.tune.mentourBiz.rest.enums.CourseStatus;
import uz.tune.mentourBiz.rest.payload.res.ResBooks;
import uz.tune.mentourBiz.rest.payload.res.ResPaymentPackage;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;
import uz.tune.mentourBiz.rest.payload.res.school.group.ResGroup;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ResCourseList {
    private UUID id;
    private String courseName;
    private String schoolName;
    private String mentorName;
    private List<ResBooks> books;
    private String moderatorName;
    private String description;
    private Integer duration;
    private Long numberOfLessons;
    private Integer studentCount;
    private String currentUnitOutOf;
    private ResGroup resGroup;
    private ResAttachment teacherAttachment;
    private ResPaymentPackage paymentPackage;
    private double unitProgresses;
    private String latestLessonName;
    private String latestUnitName;
    private Integer lastLessonIndex;
    private CourseStatus courseStatus;

    // Course duration soft limit
    private Instant courseStartDate;
    private Instant courseDueDate;
    private boolean overdue;
    private Integer daysOverdue;
    private Integer durationProgressPercent;


    public ResCourseList(Course course, Long numberOfLessons, String currentUnitOutOf, double unitProgresses) {
        this.id = course.getUuid();
        this.courseName = course.getName();
        this.description = course.getDescription();
        this.duration = course.getDuration();
        this.numberOfLessons = numberOfLessons;
        this.currentUnitOutOf = currentUnitOutOf;
        this.unitProgresses = unitProgresses;
        this.courseStatus = course.getStatus();
        if (course.getLinkedBooks() != null) {
            this.books = course.getLinkedBooks().stream().map(ResBooks::new).toList();
        }
        if (course.getSchool() != null) {
            this.schoolName = course.getSchool().getName();
        }
        if (course.getGroup() != null) {
            this.resGroup = new ResGroup(course.getGroup());
        }
        if (course.getPaymentPackage() != null) {
            this.paymentPackage = new ResPaymentPackage(course.getPaymentPackage(), null, null , null);
        }
    }

    public ResCourseList(Course course, Long numberOfLessons, String currentUnitOutOf, long studentCount, double unitProgresses) {
        this(course, numberOfLessons, currentUnitOutOf, unitProgresses);
        this.studentCount = (int) studentCount;
        if (course.getGroup() != null && course.getGroup().getTeacher() != null && course.getGroup().getTeacher().getUser().getAttachment() != null) {
            this.teacherAttachment = new ResAttachment(course.getGroup().getTeacher().getUser().getAttachment());
        }
        if (course.getPaymentPackage() != null) {
            this.paymentPackage = new ResPaymentPackage(course.getPaymentPackage(), null, null , null);
        }
    }
}