package uz.tune.mentourBiz.rest.payload.studentRes.res.exercise;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseQuestion;
import uz.tune.mentourBiz.rest.enums.ExerciseType;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.QuestionContent;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.answers.AnswerKey;

import java.util.UUID;

@Getter
@Setter
public class ResExerciseQuestion {
    private UUID uuid;
    private ExerciseType type;
    private QuestionContent content;
    private AnswerKey correctAnswer;
    private Integer coinReward;
    private Integer scoreReward;

    public ResExerciseQuestion(ExerciseQuestion question) {
        this.uuid = question.getUuid();
        this.type = question.getType();
        this.content = question.getContent();
        this.correctAnswer = (question.getAnswerKey() != null) ? question.getAnswerKey() : null; ;
        this.coinReward = question.getCoinReward();
        this.scoreReward = question.getScoreReward();
    }
}