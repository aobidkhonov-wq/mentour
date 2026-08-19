package uz.tune.mentourBiz.rest.payload.studentRes.res.exercise;

import java.util.List;
import java.util.Map;

public record ResGradingResult(
        boolean correct,
        Integer coinsEarned,
        String message,
        List<Boolean> orderingFeedback,
        Map<String, Boolean> gapFeedback,
        Map<String, Boolean> matchingFeedback,
        Integer scorePercentage
) {}