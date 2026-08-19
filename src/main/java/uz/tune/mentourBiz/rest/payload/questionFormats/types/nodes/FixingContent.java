package uz.tune.mentourBiz.rest.payload.questionFormats.types.nodes;

import lombok.Data;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.QuestionContent;

@Data
public class FixingContent extends QuestionContent {
    private String questionText;
}