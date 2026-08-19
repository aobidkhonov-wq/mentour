package uz.tune.mentourBiz.qs;

import lombok.Data;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.answers.AnswerKey;

import java.util.List;

@Data
public class CircleKey extends AnswerKey {
    private List<String> correctCharIds;
}
