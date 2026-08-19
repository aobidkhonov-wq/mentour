package uz.tune.mentourBiz.rest.payload.questionFormats.types.answers.typesOfAnswes;

import lombok.Data;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.answers.AnswerKey;

import java.util.Map;

@Data
public class MatchingKey extends AnswerKey {
    private Map<String, String> pairs;

}
