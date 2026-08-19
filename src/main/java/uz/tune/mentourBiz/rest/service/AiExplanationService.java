package uz.tune.mentourBiz.rest.service;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import uz.tune.mentourBiz.config.GlobalVar;
import uz.tune.mentourBiz.config.logger.Logger;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.rest.domain.SchoolSubscription;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.writing.WritingSubmission;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.AiStatus;
import uz.tune.mentourBiz.rest.enums.Lang;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.enums.UserStatus;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.answers.typesOfAnswes.GapFillKey;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.answers.typesOfAnswes.MatchingKey;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.answers.typesOfAnswes.OrderingKey;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.answers.typesOfAnswes.SelectionKey;
import uz.tune.mentourBiz.rest.repository.SchoolSubscriptionRepo;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.repository.unit.VocabularyAnswerRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseAnswersRepository;
import uz.tune.mentourBiz.rest.repository.user.UserRepo;
import uz.tune.mentourBiz.rest.repository.writing.WritingSubmissionRepository;
import uz.tune.mentourBiz.utils.CoreUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AiExplanationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ExerciseAnswersRepository exerciseRepo;
    private final VocabularyAnswerRepository vocabRepo;
    private final UserRepo userRepo;
    private final StudentRepo studentRepo;
    private final SchoolSubscriptionRepo schoolSubscriptionRepo;
    private final WritingSubmissionRepository writingSubmissionRepository;

    @Value("${app.ai-service.url}")
    private String aiUrl;

    @Value("${app.backend-url}")
    private String backendBaseUrl;

    @Async
    @Transactional
    public void processMistakesBatchAsync(List<UUID> exAnswerUuids, List<UUID> vocabAnswerUuids, Lang lang, String username, String requestId) {
        GlobalVar.setUsername(username);
        GlobalVar.setRequestId(requestId);
        System.out.println("ai req 2");
        User user = userRepo.findByUsernameAndStatus(username, UserStatus.ACTIVE).orElse(null);
        if (user == null) return;

        UUID schoolUuid = null;
        if (user.getRole() == UserRole.STUDENT) {
            schoolUuid = studentRepo.findByUser(user).map(s -> s.getSchool().getUuid()).orElse(null);
        }
        System.out.println("ai req 3");
        System.out.println("school uuid " + schoolUuid);

        System.out.println("testing ..........");
        List<Map<String, Object>> batchItems = new ArrayList<>();
        if (CoreUtils.isEmpty(exAnswerUuids)) {
            System.out.println("exAnswerUuids is empty");
        }
        System.out.println(" exanswers  :  " +  exAnswerUuids.size() );
        try {
            if (exAnswerUuids != null) {
                for (UUID uuid : exAnswerUuids) {
                    exerciseRepo.findBySubmissionId(uuid).ifPresent(ans -> {
                        System.out.println(ans.getAiStatus().toString());
                        System.out.println("id =  "+ans.getId());

                        if (ans.getAiStatus() == null || ans.getAiStatus() == AiStatus.NONE || ans.getAiStatus() == AiStatus.FAILED||ans.getAiStatus() == AiStatus.PROCESSING) {
                            ans.setAiStatus(AiStatus.PROCESSING);
                            exerciseRepo.saveAndFlush(ans);

                            Map<String, Object> item = new HashMap<>();
                            item.put("correlationId", uuid.toString());
                            item.put("category", "EXERCISE");
                            item.put("exerciseType", ans.getExerciseQuestion().getType().name());
                            item.put("instruction", ans.getExerciseQuestion().getInstruction());
                            item.put("questionContent", ans.getExerciseQuestion().getContent());
                            item.put("correctAnswer", extractCorrectValue(ans.getExerciseQuestion().getAnswerKey()));
                            item.put("studentAnswer", ans.getAnswerContent());
                            item.put("level", resolveExerciseLevel(ans));
                            batchItems.add(item);
                        }
                    });
                }
            }

            if (vocabAnswerUuids != null) {
                for (UUID uuid : vocabAnswerUuids) {
                    vocabRepo.findByUuid(uuid).ifPresent(ans -> {
                        if (ans.getAiStatus() == null || ans.getAiStatus() == AiStatus.NONE || ans.getAiStatus() == AiStatus.FAILED) {
                            ans.setAiStatus(AiStatus.PROCESSING);
                            vocabRepo.saveAndFlush(ans);

                            var wordEntity = ans.getVocabularyQuestion().getVocabularyWord();
                            Map<String, Object> item = new HashMap<>();
                            item.put("correlationId", uuid.toString());
                            item.put("category", "VOCABULARY");
                            item.put("targetEnglishWord", wordEntity.getWord());
                            item.put("translationsProvidedAsContext", Map.of(
                                    "uz", wordEntity.getTranslationUz(),
                                    "ru", wordEntity.getTranslationRu(),
                                    "kyrgyz", wordEntity.getTranslationKaa(),
                                    "karakalpak", wordEntity.getTranslationKrg(),
                                    "tjk", wordEntity.getTranslationTjk()
                            ));
                            item.put("studentInput", ans.getAnswerContent());
                            item.put("level", resolveVocabLevel(ans));
                            batchItems.add(item);
                        }
                    });
                }
            }

            if (batchItems.isEmpty()) return;

            Map<String, Object> payload = new HashMap<>();
            payload.put("items", batchItems);
            payload.put("uiLanguage", lang.name());
            payload.put("webhook_url", backendBaseUrl + "/api/v1/public/ai/webhook/explanation");
            System.out.println("payload = " + payload.get("webhook_url"));

            String jsonPayload = objectMapper.writeValueAsString(payload);

            Logger.logAi("OUTBOUND-EXPLAIN", aiUrl + "/api/v1/explain-mistakes", jsonPayload);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", "a1b2c3");

            Logger.logInfo(">> Requesting AI batch explanation for " + batchItems.size() + " items...");

            restTemplate.postForEntity(aiUrl + "/api/v1/explain-mistakes", new HttpEntity<>(payload, headers), Map.class);
            System.out.println("response get ");
        } catch (Exception e) {
            // Any failure (building the batch OR the HTTP call) must reset the answers,
            // otherwise they stay stuck in PROCESSING and are never retried.
            Logger.exception("Failed to process AI mistakes batch", e);
            resetStatus(exAnswerUuids, vocabAnswerUuids, AiStatus.FAILED);
        }
    }

    private String resolveExerciseLevel(uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.postExercise.ExerciseAnswers ans) {
        try {
            return ans.getExerciseTask().getUnit().getSchoolBook().getLevel().getName();
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveVocabLevel(uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.postVocabulary.VocabularyAnswer ans) {
        try {
            return ans.getVocabularySet().getUnit().getSchoolBook().getLevel().getName();
        } catch (Exception e) {
            return null;
        }
    }

    @Async
    @Transactional
    public void processWritingAiAsync(UUID submissionUuid, Lang lang, String username, String requestId) {
        GlobalVar.setUsername(username);
        GlobalVar.setRequestId(requestId);

        WritingSubmission sub = writingSubmissionRepository.findByUuid(submissionUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_SUBMISSION_NOT_FOUND.getKey()));

        sub.setAiStatus(AiStatus.PROCESSING);
        writingSubmissionRepository.saveAndFlush(sub);

        Map<String, Object> item = new HashMap<>();
        item.put("correlationId", submissionUuid.toString());
        item.put("category", "WRITING");
        item.put("instruction", sub.getExerciseQuestion().getInstruction());
        item.put("questionContent", sub.getExerciseQuestion().getContent());
        item.put("studentAnswer", sub.getEssay());
        item.put("score", sub.getExerciseQuestion().getScoreReward());
        item.put("level", sub.getStudent().getSchool().getName());

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("items", List.of(item));
            payload.put("uiLanguage", lang.name());
            payload.put("webhook_url", backendBaseUrl + "/api/v1/public/ai/webhook/explanation");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", "a1b2c3");

            restTemplate.postForEntity(aiUrl + "/api/v1/explain-mistakes", new HttpEntity<>(payload, headers), Map.class);
        } catch (Exception e) {
            sub.setAiStatus(AiStatus.FAILED);
            writingSubmissionRepository.save(sub);
            Logger.exception("Failed to send writing to AI", e);
        }
    }

    private void resetStatus(List<UUID> ex, List<UUID> vb, AiStatus status) {
        if (ex != null) ex.forEach(id -> exerciseRepo.findBySubmissionId(id).ifPresent(a -> { a.setAiStatus(status); exerciseRepo.save(a); }));
        if (vb != null) vb.forEach(id -> vocabRepo.findByUuid(id).ifPresent(a -> { a.setAiStatus(status); vocabRepo.save(a); }));
    }

    private Object extractCorrectValue(Object key) {
        if (key instanceof GapFillKey k) return k.getAnswers();
        if (key instanceof OrderingKey k) return k.getCorrectOrder();
        if (key instanceof MatchingKey k) return k.getPairs();
        if (key instanceof SelectionKey k) return k.getCorrectOptionId();
        return key;
    }
}