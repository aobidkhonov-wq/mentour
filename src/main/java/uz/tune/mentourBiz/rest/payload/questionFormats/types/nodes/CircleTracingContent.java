package uz.tune.mentourBiz.rest.payload.questionFormats.types.nodes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.QuestionContent;

import java.util.List;

@Data
public class CircleTracingContent extends QuestionContent {
    private String targetWord;
    private List<LetterData> letters;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LetterData {
        private String character;
        private List<String> svgPaths;
        private boolean isPlaceholder;
        private String placeholderId;
    }
}