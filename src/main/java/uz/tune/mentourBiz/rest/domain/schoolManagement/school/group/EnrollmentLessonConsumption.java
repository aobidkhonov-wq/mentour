package uz.tune.mentourBiz.rest.domain.schoolManagement.school.group;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.CourseLesson;

import java.util.UUID;

/**
 * Ledger row: one lesson consumed by one LESSON_PACK enrollment.
 * A row is created once per (enrollment, lesson) when the lesson is conducted, regardless of
 * whether the student attended. If the absence was excused, an admin sets {@code excused = true},
 * which returns that lesson to the student (it stops counting against the pack).
 */
@Table(
        name = "enrollment_lesson_consumptions",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_enrollment_lesson",
                columnNames = {"enrollment_id", "lesson_id"}
        )
)
@Entity
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnrollmentLessonConsumption extends BaseEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id")
    private Enrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private CourseLesson lesson;

    @Column(name = "excused")
    private boolean excused = false;
}
