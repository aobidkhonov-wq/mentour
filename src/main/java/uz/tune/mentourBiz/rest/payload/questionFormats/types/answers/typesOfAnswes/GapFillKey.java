package uz.tune.mentourBiz.rest.payload.questionFormats.types.answers.typesOfAnswes;

import lombok.Data;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.answers.AnswerKey;

import java.util.List;
import java.util.Map;

@Data
public class GapFillKey extends AnswerKey {
    private Map<String, List<String>> answers;
                        // List<answer, can, be, like, this,>
}
