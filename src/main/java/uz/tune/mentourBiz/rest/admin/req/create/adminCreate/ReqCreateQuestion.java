package uz.tune.mentourBiz.rest.admin.req.create.adminCreate;

import lombok.Data;
import uz.tune.mentourBiz.rest.enums.ExerciseType;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.QuestionContent;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.answers.AnswerKey;
import java.util.UUID;

@Data
public class ReqCreateQuestion {
    private UUID taskUuid;
    private ExerciseType type;
    private String instruction;
    private Integer coinReward;
    private Integer scoreReward;
    private QuestionContent content;
    private AnswerKey answerKey;
}