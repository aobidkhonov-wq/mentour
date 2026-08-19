package uz.tune.mentourBiz.rest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.tune.mentourBiz.qs.CircleKey;
import uz.tune.mentourBiz.rest.domain.SchoolRewardConfig;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseQuestion;
import uz.tune.mentourBiz.rest.enums.AttachmentMediaType;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.answers.typesOfAnswes.*;
import uz.tune.mentourBiz.rest.repository.SchoolRewardConfigRepo;
import uz.tune.mentourBiz.rest.repository.TracingKey;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RewardCalculationService {

    private final SchoolRewardConfigRepo rewardConfigRepo;

    public int getDynamicExerciseReward(ExerciseQuestion q, UUID schoolUuid) {
        SchoolRewardConfig config = rewardConfigRepo.findBySchool_Uuid(schoolUuid).orElse(null);

        if (config == null || !config.isExerciseAutoEnabled()) {
            return (q.getCoinReward() != null && q.getCoinReward() > 0) ? q.getCoinReward() : 5;
        }

        int baseReward = switch (q.getType()) {
            case GAP_FILL -> getCount(q) * config.getGapFillBase();         // Standard Base: 3
            case ORDERING -> getCount(q) * config.getOrderingBase();        // Standard Base: 1
            case MATCHING -> getCount(q) * config.getMatchingBase();        // Standard Base: 2
            case MULTI_SELECT -> getCount(q) * config.getMultiSelectBase(); // Standard Base: 3
            case CIRCLE -> getCount(q) * config.getCircleBase();           // Standard Base: 3
            case TRACING -> getCount(q) * config.getTracingBase();          // Standard Base: 2
            case SELECTION -> config.getSelectionBase();                   // Standard Base: 5
            case PRONUNCIATION -> 10;
            case WRITING, SPEAKING -> 30;
            default -> (q.getCoinReward() != null && q.getCoinReward() > 0) ? q.getCoinReward() : 5;
        };

        boolean hasAudio = q.getContent() != null && (
                (q.getContent().getAttachmentUrl() != null && !q.getContent().getAttachmentUrl().isBlank()) ||
                        q.getContent().getAttachmentMediaType() == AttachmentMediaType.AUDIO
        );

        if (hasAudio && config.isAudioMultiplierEnabled()) {
            return (int) Math.round(baseReward * config.getAudioMultiplier());
        }

        return baseReward;
    }

    private int getCount(ExerciseQuestion q) {
        if (q.getAnswerKey() instanceof GapFillKey k) return (k.getAnswers() != null) ? k.getAnswers().size() : 1;
        if (q.getAnswerKey() instanceof OrderingKey k) return (k.getCorrectOrder() != null) ? k.getCorrectOrder().size() : 1;
        if (q.getAnswerKey() instanceof MatchingKey k) return (k.getPairs() != null) ? k.getPairs().size() : 1;
        if (q.getAnswerKey() instanceof MultiSelectKey k) return (k.getCorrectOptionIds() != null) ? k.getCorrectOptionIds().size() : 1;
        if (q.getAnswerKey() instanceof CircleKey k) return (k.getCorrectCharIds() != null) ? k.getCorrectCharIds().size() : 1;
        if (q.getAnswerKey() instanceof TracingKey k) return (k.getPlaceholderMap() != null) ? k.getPlaceholderMap().size() : 1;
        return 1;
    }

    public int getDynamicVocabReward(UUID schoolUuid, int dbFallback) {
        return rewardConfigRepo.findBySchool_Uuid(schoolUuid)
                .filter(SchoolRewardConfig::isVocabAutoEnabled)
                .map(SchoolRewardConfig::getVocabRewardPerWord)
                .orElse(dbFallback);
    }
}