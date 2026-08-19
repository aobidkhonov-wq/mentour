package uz.tune.mentourBiz.rest.service.speaking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uz.tune.mentourBiz.rest.enums.ExerciseSubmissionStatus;
import uz.tune.mentourBiz.rest.payload.AiEvaluationPayload;
import uz.tune.mentourBiz.rest.payload.req.ReqFeedBackForSpeaking;
import uz.tune.mentourBiz.rest.payload.res.ResPronunciationAi;
import uz.tune.mentourBiz.rest.payload.res.ResSpeakingList;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResGradingResult;

import java.util.UUID;

public interface SpeakingService {
    ResponseMessage manualGrade(UUID submissionUuid, ReqFeedBackForSpeaking req);
    ResponseMessage handleAiWebhook(AiEvaluationPayload payload);
    Page<ResSpeakingList> getAllSubmissions(ExerciseSubmissionStatus status, UUID groupUuid,UUID schoolUuid, Pageable pageable);
    ResGradingResult evaluate(UUID questionUuid, UUID attachmentUuid, boolean isScoringActive);
    boolean validateAiSecret(String secret);
    ResPronunciationAi evaluatePronunciation(UUID questionUuid, UUID attachmentUuid);
    ResponseMessage rejectSpeakingSubmission(UUID submissionUuid);
}