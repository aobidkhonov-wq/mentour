package uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.postExercise;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseQuestion;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseTask;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.enums.AiStatus;

import java.util.UUID;

@Table(name = "student_answers")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseAnswers extends BaseEntity {

    @Column(name = "uuid" , unique = true, nullable = false)
    private UUID submissionId = UUID.randomUUID();

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "ever_correct")
    private Boolean everCorrect = false;

    @Column(name = "score")
    private Integer score = 0;

    @Enumerated(EnumType.STRING)
    private AiStatus aiStatus = AiStatus.NONE;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answer_content", nullable = false, columnDefinition = "jsonb")
    private String answerContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private ExerciseQuestion exerciseQuestion;


    @Column(name = "attempt_count")
    private Integer attemptCount = 0;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_task_id")
    private ExerciseTask exerciseTask;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(name = "error_explanation", columnDefinition = "TEXT")
    private String errorExplanation;

    @Column(name = "is_manually")
    private Boolean isManually = false;

    @Column(name = "updated_by")
    private String updatedBy;
}