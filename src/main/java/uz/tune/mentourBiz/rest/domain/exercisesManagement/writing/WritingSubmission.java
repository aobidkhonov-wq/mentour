package uz.tune.mentourBiz.rest.domain.exercisesManagement.writing;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseQuestion;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Teacher;
import uz.tune.mentourBiz.rest.enums.AiStatus;
import uz.tune.mentourBiz.rest.enums.ExerciseSubmissionStatus;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "writing_submissions")
public class WritingSubmission extends BaseEntity {

    @Column(name = "uuid", nullable = false, unique = true)
    private UUID uuid;

    @Column(name = "content", columnDefinition = "TEXT")
    private String essay;

    @Column(name = "score")
    private Integer score = 0;

    @Column(name = "coins_awarded")
    private Integer coinsAwarded = 0;

    @Column(name = "feedback")
    private String feedback;

    @Column(name ="status")
    @Enumerated(EnumType.STRING)
    private ExerciseSubmissionStatus status = ExerciseSubmissionStatus.PENDING_REVIEW;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne
    @JoinColumn(name = "checked_by_teacher")
    private Teacher teacher;

    @ManyToOne
    @JoinColumn(name = "exeercise_question_id")
    private ExerciseQuestion exerciseQuestion;

    @Column(name = "grammarScore")
    private Integer grammarScore = 0;
    @Column(name = "vocabularyScore")
    private Integer vocabularyScore = 0;
    @Column(name = "coherenceScore")
    private Integer coherenceScore = 0;

    @Enumerated(EnumType.STRING)
    private AiStatus aiStatus = AiStatus.NONE;

}
