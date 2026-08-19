package uz.tune.mentourBiz.rest.payload.questionFormats.types.answers.typesOfAnswes;

import lombok.Data;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.answers.AnswerKey;

@Data
public class SelectionKey extends AnswerKey {
    private String correctOptionId;
}
