package uz.tune.mentourBiz.rest.service.speaking;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.config.logger.Logger;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.external.speakingAi.ExtSpeakingAiService;
import uz.tune.mentourBiz.rest.domain.Attachment;
import uz.tune.mentourBiz.rest.domain.SchoolAcademicConfig;
import uz.tune.mentourBiz.rest.domain.SchoolSubscription;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.speaking.AiResponse;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.speaking.SpeakingScoresDto;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.speaking.SpeakingSubmission;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseQuestion;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.postExercise.ExerciseAnswers;
import uz.tune.mentourBiz.rest.domain.shopManagement.coins.CoinTransaction;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.ExerciseSubmissionStatus;
import uz.tune.mentourBiz.rest.enums.TransactionType;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.payload.AiEvaluationPayload;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.PronunciationContent;
import uz.tune.mentourBiz.rest.payload.req.ReqFeedBackForSpeaking;
import uz.tune.mentourBiz.rest.payload.res.ResPronunciationAi;
import uz.tune.mentourBiz.rest.payload.res.ResSpeakingList;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResGradingResult;
import uz.tune.mentourBiz.rest.repository.*;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.repository.transaction.CoinTransactionRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseAnswersRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseQuestionRepository;
import uz.tune.mentourBiz.rest.service.RewardCalculationService;
import uz.tune.mentourBiz.rest.service.exercise.impl.ProgressService;
import uz.tune.mentourBiz.rest.service.helper.AuthToViewEntity;
import uz.tune.mentourBiz.rest.service.user.TeacherService;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;
import uz.tune.mentourBiz.rest.service.util.FileService;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SpeakingServiceImpl implements SpeakingService {

    private final UserService userService;
    private final StudentRepo studentRepo;
    private final AttachmentRepo attachmentRepo;
    private final ExerciseQuestionRepository questionRepo;
    private final ExtSpeakingAiService aiService;
    private final SpeakingSubmissionRepository speakingRepo;
    private final FileService fileService;
    private final ExerciseAnswersRepository answerRepo;
    private final ProgressService progressService;
    private final AuthToViewEntity authToViewEntity;
    private final UserScopeService userScopeService;
    private final TeacherService teacherService;
    private final CoinTransactionRepository coinTransactionRepository;
    private final AiResponseRepository aiResponseRepository;
    private final ObjectMapper objectMapper;
    private final SchoolAcademicConfigRepo schoolAcademicConfigRepo;
    private final SchoolSubscriptionRepo schoolSubscriptionRepo;
    private final RewardCalculationService rewardCalculationService;


    @Value("${app.ai-service.webhook-secret}")
    private String aiWebhookSecret;

    @Override
    public boolean validateAiSecret(String secret) {
        return aiWebhookSecret.equals(secret);
    }

    @Override
    @Transactional
    public ResGradingResult evaluate(UUID questionUuid, UUID attachmentUuid, boolean isScoringActive) {
        User user = userService.getCurrentUser();
        Student student = studentRepo.findByUser(user).orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

        SchoolSubscription schoolSub = schoolSubscriptionRepo.findBySchool_Uuid(student.getSchool().getUuid())
                .orElseThrow(() -> new ValidationException(MessageKey.SUBSCRIPTION_EXPIRED.getKey()));

        if (!schoolSub.isAiSpeakingEnabled()) {
            throw new PermissionForbidden(MessageKey.AI_DISABLED.getKey());
        }

        ExerciseQuestion question = questionRepo.findByUuid(questionUuid).orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_QUESTION_NOT_FOUND.getKey()));
        Attachment audio = attachmentRepo.findByUuid(attachmentUuid).orElseThrow(() -> new EntityNotFoundException(MessageKey.ATTACHMENT_NOT_FOUND.getKey()));

        SpeakingSubmission sub = new SpeakingSubmission();
        sub.setStudent(student);
        sub.setExerciseQuestion(question);
        sub.setAudioAttachment(audio);
        sub.setStatus(ExerciseSubmissionStatus.PROCESSING);
        speakingRepo.save(sub);

        byte[] audioBytes = fileService.getFileBytes(audio.getFullPath());
        try {
            aiService.triggerAsyncEvaluation(
                    sub.getUuid(),
                    student.getUuid().toString(),
                    questionUuid.toString(),
                    question.getInstruction(),
                    audioBytes,
                    audio.getName(),
                    isScoringActive
            );
        } catch (Exception e) {
            Logger.exception("AI Service Trigger Failed", e);
        }

        return new ResGradingResult(false, 0, "Evaluation started. You can continue.", null, null, null, 0);
    }


    @Override
    @Transactional
    public ResponseMessage handleAiWebhook(AiEvaluationPayload payload) {
        SpeakingSubmission sub = speakingRepo.findByUuid(payload.getSubmissionUuid())
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_SUBMISSION_NOT_FOUND.getKey()));

        sub.setTranscript(payload.getEvaluationResponse().getTranscript());

        if (payload.getEvaluationResponse().getScoring() != null) {
            var sc = payload.getEvaluationResponse().getScoring();
            var scoresDto = new SpeakingScoresDto(
                    sc.getGrammar_score(),
                    sc.getVocabulary_score(),
                    sc.getCoherence_score(),
                    (sc.getGrammar_score() + sc.getVocabulary_score() + sc.getCoherence_score()) / 3
            );

            SchoolAcademicConfig config = schoolAcademicConfigRepo.findBySchool_Uuid(sub.getStudent().getSchool().getUuid())
                    .orElse(new SchoolAcademicConfig());

            Optional<ExerciseAnswers> existingAns = answerRepo.findByStudentAndExerciseQuestion(sub.getStudent(), sub.getExerciseQuestion());
            boolean alreadyPassed = existingAns.isPresent() && Boolean.TRUE.equals(existingAns.get().getEverCorrect());

            if (!alreadyPassed && scoresDto.getOverallScore() >= config.getMinScoreToPass()) {
                awardCoinsAndScore(
                        sub.getStudent(),
                        sub.getExerciseQuestion(),
                        sub.getExerciseQuestion().getCoinReward(),
                        "Speaking AI Evaluation: " + sub.getExerciseQuestion().getInstruction()
                );
            }

            sub.setScores(scoresDto);
            sub.setFeedbackBullets(sc.getFeedback_bullets());
            sub.setStatus(ExerciseSubmissionStatus.GRADED);

            saveGenericAnswer(sub.getStudent(), sub.getExerciseQuestion(), scoresDto.getOverallScore(), payload.getEvaluationResponse().getTranscript());
        } else {
            sub.setStatus(ExerciseSubmissionStatus.PENDING_REVIEW);
        }

        speakingRepo.save(sub);
        return new ResponseMessage("Successfully updated via AI Webhook");
    }

    private void saveGenericAnswer(Student student, ExerciseQuestion q, int score, String transcript) {
        ExerciseAnswers ans = answerRepo.findByStudentAndExerciseQuestion(student, q)
                .orElse(new ExerciseAnswers());

        SchoolAcademicConfig config = schoolAcademicConfigRepo.findBySchool_Uuid(student.getSchool().getUuid())
                .orElse(new SchoolAcademicConfig());

        boolean isNowCorrect = score >= config.getMinScoreToPass();
        boolean wasEverCorrect = ans.getEverCorrect() != null && ans.getEverCorrect();

        ans.setStudent(student);
        ans.setExerciseQuestion(q);
        ans.setIsCorrect(isNowCorrect);
        ans.setEverCorrect(wasEverCorrect || isNowCorrect);
        ans.setScore(Math.max(ans.getScore() != null ? ans.getScore() : 0, score));

        String content = (transcript != null && !transcript.isBlank()) ? transcript : "Manual Grade - No Transcript";
        ans.setAnswerContent("\"" + content.replace("\"", "\\\"") + "\"");

        if (q.getExerciseTask() != null && !q.getExerciseTask().isEmpty()) {
            ans.setExerciseTask(q.getExerciseTask().get(0));
            answerRepo.save(ans);
            if (ans.getExerciseTask().getUnit() != null) {
                progressService.updateUnitProgress(student, ans.getExerciseTask().getUnit().getUuid());
            }
        } else {
            answerRepo.save(ans);
        }
    }

    @Override
    @Transactional
    public ResPronunciationAi evaluatePronunciation(UUID questionUuid, UUID attachmentUuid) {
        User user = userService.getCurrentUser();
        Student student = studentRepo.findByUser(user)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

        ExerciseQuestion question = questionRepo.findByUuid(questionUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_QUESTION_NOT_FOUND.getKey()));

        Attachment audio = attachmentRepo.findByUuid(attachmentUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.ATTACHMENT_NOT_FOUND.getKey()));

        if (!(question.getContent() instanceof PronunciationContent content)) {
            throw new ValidationException(MessageKey.PRONUNCIATION_INVALID.getKey());
        }

        int maxTries = 3;
        try {
            if (content.getMaxTries() != null) {
                maxTries = Integer.parseInt(content.getMaxTries());
            }
        } catch (NumberFormatException e) {
            Logger.logWarn("Invalid maxTries format for question " + questionUuid + ". Defaulting to 3.");
        }

        SpeakingSubmission sub = speakingRepo.findByStudent_User_UuidAndExerciseQuestion_Uuid(user.getUuid(), questionUuid)
                .orElseGet(() -> {
                    SpeakingSubmission newSub = new SpeakingSubmission();
                    newSub.setStudent(student);
                    newSub.setExerciseQuestion(question);
                    newSub.setAttemptCount(0);
                    newSub.setScores(new SpeakingScoresDto(0, 0, 0, 0));
                    return speakingRepo.saveAndFlush(newSub);
                });

        if (sub.getStatus().equals(ExerciseSubmissionStatus.GRADED) && sub.getAttemptCount() >= maxTries) {
            throw new ValidationException(MessageKey.EXERCISE_MAX_RETRIES.getKey());
        }

        if (sub.getScores() != null && sub.getScores().getOverallScore() == 100) {
            throw new ValidationException(MessageKey.PRONUNCIATION_MASTERED.getKey());
        }

        if (sub.getAttemptCount() >= maxTries) {
            throw new ValidationException(MessageKey.EXERCISE_MAX_RETRIES.getKey());
        }


        byte[] audioBytes = fileService.getFileBytes(audio.getFullPath());
        ResPronunciationAi aiResult = aiService.checkPronunciation(content.getTargetWord(), audioBytes, audio.getName());

        AiResponse aiResponse = aiResponseRepository.findBySpeakingSubmission(sub)
                .orElse(new AiResponse());

        if (aiResult == null || aiResult.getProcessedWords() == null || aiResult.getProcessedWords().isEmpty()) {
            throw new ValidationException(MessageKey.AI_SPEECH_FAILED.getKey());
        }

        try {
            aiResponse.setSpeakingSubmission(sub);
            aiResponse.setResponseJson(objectMapper.writeValueAsString(aiResult));
            aiResponseRepository.save(aiResponse);
        } catch (JsonProcessingException e) {
            aiResponse.setResponseJson("{\"error\":\"Serialization failed\"}");
        }

        double currentAccuracy = aiResult.getProcessedWords().get(0).getPronunciationAssessment().getAccuracyScore();
        int currentScore = (int) Math.round(currentAccuracy);

        sub.setAttemptCount(sub.getAttemptCount() + 1);
        int bestScore = Math.max(sub.getScores().getOverallScore(), currentScore);
        sub.getScores().setOverallScore(bestScore);

        sub.setAudioAttachment(audio);
        sub.setPronunciationData(aiResult);
        sub.setStatus(ExerciseSubmissionStatus.GRADED);

        sub = speakingRepo.saveAndFlush(sub);

        updatePronunciationAnswerRecord(student, question, bestScore, sub.getAttemptCount(), maxTries);

        if(sub.getScores().getOverallScore() >= 100){
            content.setMaxTries(String.valueOf(0));
        }

        return aiResult;
    }

    private void updatePronunciationAnswerRecord(Student student, ExerciseQuestion q, int bestScore, int currentAttempts, int maxTries) {
        ExerciseAnswers ans = answerRepo.findByStudentAndExerciseQuestion(student, q)
                .orElse(new ExerciseAnswers());

        SchoolAcademicConfig config = schoolAcademicConfigRepo.findBySchool_Uuid(student.getSchool().getUuid())
                .orElse(new SchoolAcademicConfig());

        boolean previouslyPassed = Boolean.TRUE.equals(ans.getEverCorrect());
        boolean isNowCorrect = bestScore >= config.getMinScoreToPass();

        if (ans.getId() == null) {
            ans.setStudent(student);
            ans.setExerciseQuestion(q);
            if (q.getExerciseTask() != null && !q.getExerciseTask().isEmpty()) {
                ans.setExerciseTask(q.getExerciseTask().get(0));
            }
        }

        if (!previouslyPassed && isNowCorrect) {
            awardCoinsAndScore(student, q, q.getCoinReward(), "Pronunciation Mastered: " + q.getInstruction());
        }

        int possibleReward = q.getScoreReward() != null ? q.getScoreReward() : 0;
        int weightedScore = (int) ((bestScore / 100.0) * possibleReward);
        ans.setScore(Math.max(ans.getScore() != null ? ans.getScore() : 0, weightedScore));
        ans.setAnswerContent("\"Pronunciation attempt " + currentAttempts + "\"");

        ans.setIsCorrect(isNowCorrect);
        if (isNowCorrect || currentAttempts >= maxTries) {
            ans.setEverCorrect(true);
        }

        answerRepo.save(ans);

        if (ans.getExerciseTask() != null && ans.getExerciseTask().getUnit() != null) {
            progressService.updateUnitProgress(student, ans.getExerciseTask().getUnit().getUuid());
        }
    }


    @Override
    @Transactional
    public ResponseMessage manualGrade(UUID submissionUuid, ReqFeedBackForSpeaking req) {
        SpeakingSubmission sub = speakingRepo.findByUuid(submissionUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_SUBMISSION_NOT_FOUND.getKey()));

        User currentUser = userService.getCurrentUser();
        Student student = sub.getStudent();

        authToViewEntity.authorizeActionUponStudent(student);

        var scoresDto = new SpeakingScoresDto(
                req.getGrammarScore(),
                req.getVocabularyScore(),
                req.getCoherenceScore(),
                (req.getGrammarScore() + req.getVocabularyScore() + req.getCoherenceScore()) / 3
        );
        sub.setScores(scoresDto);
        sub.setFeedbackBullets(List.of(req.getFeedback()));
        sub.setStatus(ExerciseSubmissionStatus.GRADED);
        speakingRepo.save(sub);

        ExerciseAnswers ans = answerRepo.findByStudentAndExerciseQuestion(student, sub.getExerciseQuestion())
                .orElse(new ExerciseAnswers());

        if (ans.getId() == null) {
            ans.setStudent(student);
            ans.setExerciseQuestion(sub.getExerciseQuestion());
            if (sub.getExerciseQuestion().getExerciseTask() != null && !sub.getExerciseQuestion().getExerciseTask().isEmpty()) {
                ans.setExerciseTask(sub.getExerciseQuestion().getExerciseTask().get(0));
            }
        }

        String transcriptValue = (sub.getTranscript() != null && !sub.getTranscript().isBlank())
                ? sub.getTranscript() : "Manual Grade";
        ans.setAnswerContent("\"" + transcriptValue.replace("\"", "\\\"") + "\"");

        SchoolAcademicConfig config = schoolAcademicConfigRepo.findBySchool_Uuid(student.getSchool().getUuid())
                .orElse(new SchoolAcademicConfig());

        boolean passed = scoresDto.getOverallScore() >= config.getMinScoreToPass();
        int possibleReward = sub.getExerciseQuestion().getScoreReward() != null ? sub.getExerciseQuestion().getScoreReward() : 0;
        int weightedScore = (int) ((scoresDto.getOverallScore() / 100.0) * possibleReward);

        ans.setIsCorrect(passed);
        ans.setScore(weightedScore);

        if (passed && !Boolean.TRUE.equals(ans.getEverCorrect())) {
            ans.setEverCorrect(true);

            int dynamicFallback = rewardCalculationService.getDynamicExerciseReward(
                    sub.getExerciseQuestion(), student.getSchool().getUuid());

            Integer coinsToAward = (req.getCoins() != null) ? req.getCoins() : dynamicFallback;

            //
            student.setCoins(student.getCoins() + coinsToAward);
            student.setLifeTimeCoinBalance(student.getLifeTimeCoinBalance() + (long) coinsToAward);
            student.setScore(student.getScore() + weightedScore);
            studentRepo.save(student);

            CoinTransaction tx = new CoinTransaction();
            tx.setStudent(student);
            tx.setAmount(coinsToAward);
            tx.setType(TransactionType.ADMINISTRATION_AWARD);
            tx.setReason("Speaking Graded: " + sub.getExerciseQuestion().getInstruction());
            tx.setGivenBy(currentUser.getUsername());
            coinTransactionRepository.save(tx);
        }

        answerRepo.save(ans);

        if (ans.getExerciseTask() != null && ans.getExerciseTask().getUnit() != null) {
            progressService.updateUnitProgress(student, ans.getExerciseTask().getUnit().getUuid());
        }

        return new ResponseMessage("Speaking submission graded manually.");
    }

    @Override
    @Transactional
    public ResponseMessage rejectSpeakingSubmission(UUID submissionUuid) {
        SpeakingSubmission sub = speakingRepo.findByUuid(submissionUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_SUBMISSION_NOT_FOUND.getKey()));

        authToViewEntity.authorizeActionUponStudent(sub.getStudent());

        // 1. Delete associated AI Responses if they exist
        aiResponseRepository.findBySpeakingSubmission(sub).ifPresent(aiResponseRepository::delete);

        // 2. Remove from ExerciseAnswers
        answerRepo.findByStudentAndExerciseQuestion(sub.getStudent(), sub.getExerciseQuestion())
                .ifPresent(answerRepo::delete);

        // 3. Delete the SpeakingSubmission record
        speakingRepo.delete(sub);

        // 4. Update progress
        progressService.updateUnitProgress(sub.getStudent(),
                sub.getExerciseQuestion().getExerciseTask().get(0).getUnit().getUuid());

        return new ResponseMessage("Speaking attempt rejected and deleted.");
    }


    @Override
    @Transactional(readOnly = true)
    public Page<ResSpeakingList> getAllSubmissions(ExerciseSubmissionStatus status, UUID groupUuid, UUID schoolUuid ,Pageable pageable) {
        User user = userService.getCurrentUser();

        UUID resolvedId = userScopeService.resolveSchoolUuid(schoolUuid);

        Collection<UUID> schoolUuids;
        if (resolvedId != null) {
            schoolUuids = List.of(resolvedId);
        } else {
            schoolUuids = userScopeService.getAuthorizedSchoolUuids();
        }

        UUID teacherUuid = user.getRole().equals(UserRole.TEACHER) ? user.getUuid() : null;
        return speakingRepo.findAllFilteredMulti(schoolUuids, status, groupUuid, teacherUuid ,pageable).map(ResSpeakingList::new);
    }

    public void awardCoinsAndScore(Student student, ExerciseQuestion q, Integer coinAmount, String reason) {
        student.setCoins(student.getCoins() + coinAmount);
        student.setLifeTimeCoinBalance(student.getLifeTimeCoinBalance() + coinAmount);

        if (q.getScoreReward() != null) {
            student.setScore(student.getScore() + q.getScoreReward());
        }

        studentRepo.save(student);

        CoinTransaction tx = new CoinTransaction();
        tx.setStudent(student);
        tx.setAmount(coinAmount);
        tx.setType(TransactionType.EXERCISE_AWARD);
        tx.setReason(reason);
        tx.setComment(reason);
        coinTransactionRepository.save(tx);
    }
}