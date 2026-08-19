package uz.tune.mentourBiz.rest.payload.studentRes.res.exercise;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class ResVocabSetResult {
    private UUID setUuid;
    private String setTitle;
    private int totalWords;
    private int correctCount;
    private int coinsEarned;
    private int totalScoreEarned;
    private int percentage;
    private boolean aiExplanationEnabled;

    private List<WordResultSummary> words;

    @Getter
    @Setter
    @AllArgsConstructor
    public static class WordResultSummary {
        private UUID wordUuid;
        private String attachmentUrl;
        private String word;
        private Integer coinReward;
        private Integer scoreEarned;
        private boolean isCorrect;
        private String studentAnswer;
        private String explanation;
    }
}