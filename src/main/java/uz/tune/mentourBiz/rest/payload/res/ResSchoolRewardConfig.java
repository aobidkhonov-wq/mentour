package uz.tune.mentourBiz.rest.payload.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.tune.mentourBiz.rest.domain.SchoolRewardConfig;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResSchoolRewardConfig {
    private boolean exerciseAutoEnabled;
    private int gapFillBase;
    private int orderingBase;
    private int matchingBase;
    private int selectionBase;
    private int multiSelectBase;
    private int circleBase;
    private int tracingBase;
    private boolean audioMultiplierEnabled;
    private double audioMultiplier;
    private boolean vocabAutoEnabled;
    private int vocabRewardPerWord;

    public ResSchoolRewardConfig(SchoolRewardConfig config) {
        this.exerciseAutoEnabled = config.isExerciseAutoEnabled();
        this.gapFillBase = config.getGapFillBase();
        this.orderingBase = config.getOrderingBase();
        this.matchingBase = config.getMatchingBase();
        this.selectionBase = config.getSelectionBase();
        this.multiSelectBase = config.getMultiSelectBase();
        this.circleBase = config.getCircleBase();
        this.tracingBase = config.getTracingBase();
        this.audioMultiplierEnabled = config.isAudioMultiplierEnabled();
        this.audioMultiplier = config.getAudioMultiplier();
        this.vocabAutoEnabled = config.isVocabAutoEnabled();
        this.vocabRewardPerWord = config.getVocabRewardPerWord();
    }
}