package uz.tune.mentourBiz.rest.payload.res;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class ResPronunciationAi {

    @JsonProperty("word_requested")
    private String wordRequested;

    @JsonProperty("total_words")
    private int totalWords;

    @JsonProperty("processed_words")
    private List<ProcessedWord> processedWords;

    @Data
    public static class ProcessedWord {
        @JsonProperty("Word")
        private String word;

        @JsonProperty("PronunciationAssessment")
        private Assessment pronunciationAssessment;

        @JsonProperty("Syllables")
        private List<Syllable> syllables;
    }

    @Data
    public static class Assessment {
        @JsonProperty("AccuracyScore")
        private double accuracyScore;
    }

    @Data
    public static class Syllable {
        @JsonProperty("Syllable")
        private String syllable;

        @JsonProperty("Grapheme")
        private String grapheme;

        @JsonProperty("PronunciationAssessment")
        private Assessment pronunciationAssessment;
    }
}