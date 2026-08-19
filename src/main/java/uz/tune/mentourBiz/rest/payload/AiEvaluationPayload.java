package uz.tune.mentourBiz.rest.payload;

import lombok.Data;
import uz.tune.mentourBiz.external.speakingAi.ExtSpeakingAiService;

import java.util.UUID;

@Data
public class AiEvaluationPayload {
    private UUID submissionUuid;
    private ExtSpeakingAiService.AiEvaluationResponse evaluationResponse;
}