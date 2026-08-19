package uz.tune.mentourBiz.external.speakingAi;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import uz.tune.mentourBiz.rest.payload.res.ResPronunciationAi;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExtSpeakingAiService {
    private final RestTemplate restTemplate;

    @Value("${app.ai-service.url}")
    private String aiUrl;


    public void triggerAsyncEvaluation(UUID submissionUuid, String userId, String questionId, String text, byte[] fileBytes, String fileName, boolean scoringActive) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-API-Key", "a1b2c3");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("user_id", userId);
        body.add("question_id ", submissionUuid.toString());
        body.add("speech_question ", text);
        body.add("is_scoring_active", scoringActive);

        ByteArrayResource resource = new ByteArrayResource(fileBytes) {
            @Override public String getFilename() { return fileName; }
        };
        body.add("file", resource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        restTemplate.postForEntity(aiUrl + "/api/v1/evaluate", requestEntity, String.class);
    }

    public ResPronunciationAi checkPronunciation(String word, byte[] fileBytes, String fileName) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-API-Key", "a1b2c3");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("word", word);

        ByteArrayResource resource = new ByteArrayResource(fileBytes) {
            @Override public String getFilename() { return fileName; }
        };
        body.add("file", resource);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        String url = "http://185.183.241.197:8000/api/v1/word-pronun-checker";
        return restTemplate.postForObject(url, requestEntity, ResPronunciationAi.class);
    }



    @Data
    public static class AiEvaluationResponse {
        private String transcript;
        private ScoringResult scoring;
    }

    @Data
    public static class ScoringResult {
        private int grammar_score;
        private int vocabulary_score;
        private int coherence_score;
        private List<String> feedback_bullets;
    }
}