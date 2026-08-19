package uz.tune.mentourBiz.rest.payload.questionFormats.types.nodes;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.QuestionContent;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class MatchingContent extends QuestionContent {
    private List<Map<String, String>> rightItems;
    private List<Map<String, String>> leftItems;

}
