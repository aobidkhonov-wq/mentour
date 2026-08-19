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
public class ResAdminQuestions {
    private UUID uuid;
    private ExerciseType type;
    private QuestionContent content;
    private AnswerKey correctAnswer;
    private Integer coinAmount;
    private Integer scoreReward;

    public ResAdminQuestions(ExerciseQuestion question) {
        this.uuid = question.getUuid();
        this.type = question.getType();
        this.content = question.getContent();
        this.correctAnswer = (question.getAnswerKey() != null) ? question.getAnswerKey() : null;
        this.coinAmount = question.getCoinReward();
        this.scoreReward = question.getScoreReward();
    }
}