package uz.tune.mentourBiz.rest.payload.questionFormats.types.nodes;

import lombok.Data;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.QuestionContent;

@Data
public class SpeakingContent extends QuestionContent {
    private String speakingPrompt;
    private Long seconds;
}