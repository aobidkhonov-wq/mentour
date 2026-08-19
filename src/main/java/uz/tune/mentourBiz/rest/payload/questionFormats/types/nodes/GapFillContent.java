package uz.tune.mentourBiz.rest.payload.questionFormats.types.nodes;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uz.tune.mentourBiz.rest.enums.GapFillMode;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.QuestionContent;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class GapFillContent extends QuestionContent {
    private String text;
    private List<Map<String, InputConfig>> inputs;

    @Data
    public static class InputConfig {
        private GapFillMode mode; // TEXT or DROP
        private List<String> options; // mull for text , 3~ for dropdown
        private String hint; // optional
    }
}
