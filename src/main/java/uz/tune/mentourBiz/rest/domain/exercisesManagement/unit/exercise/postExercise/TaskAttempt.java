package uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.postExercise;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseTask;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;

import java.util.UUID;

/**
 * Tracks how many attempts a single student has spent on a single homework task.
 * The effective allowance is the school's {@code homeworkAttemptLimit} plus any
 * {@code extraAttempts} granted by a teacher.
 */
@Table(name = "task_attempts", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "task_id"}))
@Entity
@Getter
@Setter
@NoArgsConstructor
public class TaskAttempt extends BaseEntity {

    @Column(name = "uuid", unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private ExerciseTask task;

    @Column(name = "attempts_used")
    private int attemptsUsed = 0;

    // Additional attempts a teacher granted on top of the school limit.
    @Column(name = "extra_attempts")
    private int extraAttempts = 0;
}
