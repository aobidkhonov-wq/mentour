package uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uz.tune.mentourBiz.base.BaseEntity;
import uz.tune.mentourBiz.rest.enums.ExerciseType;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.QuestionContent;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.answers.AnswerKey;

import java.util.List;
import java.util.UUID;

@Table(name = "questions")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseQuestion extends BaseEntity {

    @Column(name = "uuid" , unique = true, nullable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "coin_reward")
    private Integer coinReward;

    @Column(name = "score_reward")
    private Integer scoreReward;

    @Column(name = "exercise_type")
    @Enumerated(EnumType.STRING)
    private ExerciseType type;

    @Column(name = "instruction", columnDefinition = "TEXT")
    private String instruction;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", columnDefinition = "jsonb")
    private QuestionContent content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answer_key", columnDefinition = "jsonb")
    private AnswerKey answerKey;

    @ManyToMany
    @JoinTable(
            name = "exercise_questions",
            joinColumns = @JoinColumn(name = "questions_id"),
            inverseJoinColumns = @JoinColumn(name = "exercise_task_id")
    )
    private List<ExerciseTask> exerciseTask;



}
