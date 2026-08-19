package uz.tune.mentourBiz.rest.domain.schoolManagement.schedule;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.CourseLesson;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.AttendanceStatus;

import java.util.UUID;

@Table(name = "attendance_records")
@Entity
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttendanceRecord extends BaseEntity {

    @Column(name = "uuid" , unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name="attendance_status")
    @Enumerated(EnumType.STRING)
    private AttendanceStatus status = AttendanceStatus.ABSENT;

    @Column(name="is_marked_by_teacher")
    private Boolean isMarked = false;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_lesson_id")
    private CourseLesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="marked_by_user_id")
    private User markedBy;

    @Column(name = "is_attendance_notified")
    private Boolean isAttendanceNotified = false;

    @Column(name = "is_unit_notified")
    private Boolean isUnitNotified = false;
}