package uz.tune.mentourBiz.rest.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.tune.mentourBiz.config.logger.Logger;
import uz.tune.mentourBiz.rest.enums.AiStatus;
import uz.tune.mentourBiz.rest.repository.unit.VocabularyAnswerRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseAnswersRepository;
import uz.tune.mentourBiz.rest.repository.writing.WritingSubmissionRepository;
import uz.tune.mentourBiz.rest.service.writing.WritingService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/ai/webhook")
@RequiredArgsConstructor
public class AiWebhookEndpoint {

    private final ExerciseAnswersRepository exerciseRepo;
    private final VocabularyAnswerRepository vocabRepo;
    private final WritingSubmissionRepository writingSubmissionRepository;
    private final WritingService writingService;

    @PostMapping("/explanation")
    public void handleWebhook(@RequestBody Map<String, Object> payload) {

        try {
            Logger.logAi("INBOUND-WEBHOOK-EXPLAIN", "/api/v1/public/ai/webhook/explanation",
                    new ObjectMapper().writeValueAsString(payload));
        } catch (Exception e) {
            Logger.logWarn("Could not log AI Webhook payload");
        }

        List<Map<String, Object>> results = (List<Map<String, Object>>) payload.get("results");
        for (Map<String, Object> res : results) {
            UUID uuid = UUID.fromString(String.valueOf(res.get("correlationId")));

            if (res.containsKey("score")) {
                writingService.processAiResult(
                        uuid,
                        ((Number) res.get("score")).intValue(),
                        (String) res.get("explanation"),
                        ((Number) res.get("grammar_score")).intValue(),
                        ((Number) res.get("vocabulary_score")).intValue(),
                        ((Number) res.get("coherence_score")).intValue()
                );
            } else {
                String text = (String) res.get("explanation");
                exerciseRepo.findBySubmissionId(uuid).ifPresent(ans -> {
                    ans.setErrorExplanation(text);
                    ans.setAiStatus(AiStatus.COMPLETED);
                    exerciseRepo.save(ans);
                });
                vocabRepo.findByUuid(uuid).ifPresent(ans -> {
                    ans.setErrorExplanation(text);
                    ans.setAiStatus(AiStatus.COMPLETED);
                    vocabRepo.save(ans);
                });
            }
        }
    }
}