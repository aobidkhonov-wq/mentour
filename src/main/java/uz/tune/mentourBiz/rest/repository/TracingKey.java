package uz.tune.mentourBiz.rest.repository;

import lombok.Data;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.answers.AnswerKey;

import java.util.Map;

@Data
public class TracingKey extends AnswerKey {
    private Map<String, String> placeholderMap;
}
