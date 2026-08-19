package uz.tune.mentourBiz.qs;

import lombok.Data;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.QuestionContent;

import java.util.List;

@Data
public class CircleContent extends QuestionContent {
    private List<CircleWord> parts;

    @Data
    public static class CircleWord {
        private String wordId;
        private List<CircleChar> chars;
        private boolean isSpace = false;
    }

    @Data
    public static class CircleChar {
        private String id;
        private String value;
    }
}
