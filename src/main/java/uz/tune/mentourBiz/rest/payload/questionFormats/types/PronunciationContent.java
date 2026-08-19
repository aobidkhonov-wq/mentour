package uz.tune.mentourBiz.rest.payload.questionFormats.types;

import lombok.Data;

@Data
public class PronunciationContent extends QuestionContent {
    private String targetWord;
    private String maxTries;
}