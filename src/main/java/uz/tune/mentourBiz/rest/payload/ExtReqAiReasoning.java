package uz.tune.mentourBiz.rest.payload;

import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import uz.tune.mentourBiz.rest.enums.ExerciseType;
import uz.tune.mentourBiz.rest.enums.Lang;

@Data
@Builder
@Setter
public class ExtReqAiReasoning {
    private String instruction;
    private Object questionContent;
    private ExerciseType exerciseType;
    private Object correctAnswer;
    private Object studentAnswer;
    private String complexityLevel;
    private Lang studentUiLanguage;
}