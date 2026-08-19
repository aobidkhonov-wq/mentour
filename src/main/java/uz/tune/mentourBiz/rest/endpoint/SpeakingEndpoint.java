package uz.tune.mentourBiz.rest.endpoint;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.rest.enums.ExerciseSubmissionStatus;
import uz.tune.mentourBiz.rest.model.ResAttachmentModel;
import uz.tune.mentourBiz.rest.payload.AiEvaluationPayload;
import uz.tune.mentourBiz.rest.payload.req.ReqFeedBackForSpeaking;
import uz.tune.mentourBiz.rest.payload.res.ResPronunciationAi;
import uz.tune.mentourBiz.rest.payload.res.ResSpeakingList;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResGradingResult;
import uz.tune.mentourBiz.rest.service.speaking.SpeakingService;
import uz.tune.mentourBiz.rest.service.util.FileService;

import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + BaseURI.SPEAKING)
@RequiredArgsConstructor
public class SpeakingEndpoint {

    private final SpeakingService speakingService;
    private final FileService fileService;

    @PostMapping(value = "/upload/speaking", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResAttachmentModel> upload(@RequestParam MultipartFile file) {
        return ResponseEntity.ok(fileService.uploadSpeakingAudio(file));
    }

    @PostMapping("/submissions/{uuid}/reject")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).SPEAKING_REJECT)")
    public ResponseEntity<ResponseMessage> rejectSpeaking(@PathVariable UUID uuid) {
        return ResponseEntity.ok(speakingService.rejectSpeakingSubmission(uuid));
    }

    @PostMapping("/evaluate")
    public ResponseEntity<ResGradingResult> evaluate(
            @RequestParam UUID questionUuid,
            @RequestParam UUID attachmentUuid,
            @RequestParam boolean isScoringActive) {
        return ResponseEntity.ok(speakingService.evaluate(questionUuid, attachmentUuid, isScoringActive));
    }

    @PostMapping("/webhook/ai")
    public ResponseEntity<ResponseMessage> aiWebhook(
            @RequestHeader("X-AI-Secret") String secret,
            @RequestBody AiEvaluationPayload payload) {

        if (!speakingService.validateAiSecret(secret)) {
            throw new PermissionForbidden(MessageKey.TOKEN_INVALID.getKey());
        }

        return ResponseEntity.ok(speakingService.handleAiWebhook(payload));
    }

    @GetMapping("/submissions")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).SUBMISSIONS_GET)")
    public ResponseEntity<Page<ResSpeakingList>> getSubmissions(
            @RequestParam(required = false) ExerciseSubmissionStatus status,
            @RequestParam(required = false) UUID groupUuid,
            @RequestParam(required = false) UUID schoolUuid,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(speakingService.getAllSubmissions(status, groupUuid, schoolUuid,pageable));
    }

    @PostMapping("/evaluate/pronunciation")
    public ResponseEntity<ResPronunciationAi> evaluatePronunciation(
            @RequestParam UUID questionUuid,
            @RequestParam UUID attachmentUuid) {
        return ResponseEntity.ok(speakingService.evaluatePronunciation(questionUuid, attachmentUuid));
    }

    @PostMapping("/submissions/{uuid}/grade")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).MANUAL_GRADE)")
    public ResponseEntity<ResponseMessage> manualGrade(
            @PathVariable UUID uuid,
            @RequestBody ReqFeedBackForSpeaking req) {
        return ResponseEntity.ok(speakingService.manualGrade(uuid, req));
    }
}