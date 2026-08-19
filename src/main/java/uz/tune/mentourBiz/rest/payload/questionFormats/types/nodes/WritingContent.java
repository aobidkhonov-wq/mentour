package uz.tune.mentourBiz.rest.payload.questionFormats.types.nodes;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.QuestionContent;

@Data
@EqualsAndHashCode(callSuper = true)
public class WritingContent extends QuestionContent {
    private String writingQuestion;
    private Integer minWords;
    private Long deadLine;
}
