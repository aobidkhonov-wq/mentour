package uz.tune.mentourBiz.rest.payload.questionFormats.types.answers.typesOfAnswes;

import lombok.Data;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.answers.AnswerKey;

import java.util.List;


@Data
public class OrderingKey extends AnswerKey {
    private List<String> correctOrder;
}
