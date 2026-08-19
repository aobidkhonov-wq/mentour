package uz.tune.mentourBiz.rest.payload.questionFormats.types.nodes;

import lombok.Data;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.QuestionContent;

import java.util.List;
import java.util.Map;

@Data
public class MultiSelectContent extends QuestionContent {
    private String question;
    private String text;
    private List<Map<String, String>> options;
}