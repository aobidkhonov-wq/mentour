package uz.tune.mentourBiz.rest.domain.schoolManagement.course;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Teacher;
import uz.tune.mentourBiz.rest.enums.LessonStatus;
import uz.tune.mentourBiz.rest.enums.LessonType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Table(name = "course_lessons")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseLesson extends BaseEntity {

    @Column(name = "uuid")
    private UUID uuid = UUID.randomUUID();

    @Column(name = "name")
    private String name;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "link", columnDefinition = "varchar(500)")
    private String link;

    @Formula("CASE " +
            "WHEN status = 'STARTED' THEN 1 " +
            "WHEN status = 'NEW' THEN 2 " +
            "WHEN status = 'FINISHED' THEN 3 " +
            "ELSE 4 " +
            "END")
    private int statusOrder;

    @Column(name = "bbb_meeting_id")
    private String bbbMeetingId;

    @Column(name = "description",length = 1000)
    private String description;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private LessonStatus status;

    @Column(name = "type_lesson")
    @Enumerated(EnumType.STRING)
    private LessonType lessonType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    // The teacher who actually conducted this lesson. Null means the group's regular teacher taught it.
    // Set to a different teacher when someone substitutes, so payroll credits that lesson to the
    // substitute instead of the group's default teacher.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conducted_by_teacher_id")
    private Teacher conductedByTeacher;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "lesson_units_link",
            joinColumns = @JoinColumn(name = "lesson_id"),
            inverseJoinColumns = @JoinColumn(name = "unit_id")
    )
    private List<Unit> units = new ArrayList<>();
}