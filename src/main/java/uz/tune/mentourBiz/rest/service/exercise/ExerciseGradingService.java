package uz.tune.mentourBiz.rest.service.exercise;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.qs.CircleKey;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseQuestion;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseTask;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.postExercise.ExerciseAnswers;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.writing.WritingSubmission;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.enums.ExerciseSubmissionStatus;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.answers.AnswerKey;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.answers.typesOfAnswes.*;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.nodes.WritingContent;
import uz.tune.mentourBiz.rest.payload.studentReq.req.exercise.ReqExerciseSubmit;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResGradingResult;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseAnswersRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseTaskRepository;
import uz.tune.mentourBiz.rest.repository.writing.WritingSubmissionRepository;
import uz.tune.mentourBiz.utils.CoreUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ExerciseGradingService {

    private final WritingSubmissionRepository writingSubmissionRepository;
    private final ExerciseAnswersRepository exerciseAnswersRepository;
    private final ExerciseTaskRepository taskRepo;
    private final ObjectMapper objectMapper;

    public ResGradingResult grade(ExerciseQuestion question, ReqExerciseSubmit submit) {
        AnswerKey key = question.getAnswerKey();
        boolean isCorrect = false;
        int scorePercentage = 0;
        List<Boolean> orderingFeedback = null;
        Map<String, Boolean> gapFeedback = null;
        Map<String, Boolean> matchingFeedback = null;

        switch (submit.getType()) {
            case GAP_FILL:
                GapFillKey gapKey = (GapFillKey) key;
                gapFeedback = gradeGapFill(gapKey, submit.getAnswers());
                long totalGaps = gapKey.getAnswers().size();
                long correctGaps = gapFeedback.values().stream().filter(Boolean.TRUE::equals).count();
                scorePercentage = (int) (((double) correctGaps / totalGaps) * 100);
                isCorrect = scorePercentage == 100;
                break;
            case MATCHING:
                MatchingKey matchKey = (MatchingKey) key;
                matchingFeedback = gradeMatching(matchKey, submit.getMatchingPairs());
                long totalPairs = matchKey.getPairs().size();
                long correctPairs = matchingFeedback.values().stream().filter(Boolean.TRUE::equals).count();
                scorePercentage = (int) (((double) correctPairs / totalPairs) * 100);
                isCorrect = scorePercentage == 100;
                break;
            case SELECTION:
                isCorrect = gradeSelection((SelectionKey) key, submit.getSelectedOptionId());
                scorePercentage = isCorrect ? 100 : 0;
                break;
            case ORDERING:
                OrderingKey orderKey = (OrderingKey) key;
                orderingFeedback = getGranularOrderingFeedback(orderKey, submit.getOrderingAnswer());
                long totalItems = orderKey.getCorrectOrder().size();
                long correctItems = orderingFeedback.stream().filter(Boolean.TRUE::equals).count();
                scorePercentage = (int) (((double) correctItems / totalItems) * 100);
                isCorrect = scorePercentage == 100;
                break;
            case MULTI_SELECT:
                MultiSelectKey multiKey = (MultiSelectKey) question.getAnswerKey();
                List<String> studentAns = submit.getMultiSelectAnswer();

                if (studentAns != null && studentAns.size() == multiKey.getCorrectOptionIds().size()) {
                    isCorrect = new HashSet<>(studentAns).equals(new HashSet<>(multiKey.getCorrectOptionIds()));
                }
                scorePercentage = isCorrect ? 100 : 0;
                break;
            case CIRCLE:
                CircleKey circleKey = (CircleKey) key;
                List<String> circleAns = submit.getMultiSelectAnswer();
                if (circleAns != null && circleAns.size() == circleKey.getCorrectCharIds().size()) {
                    isCorrect = new HashSet<>(circleAns).equals(new HashSet<>(circleKey.getCorrectCharIds()));
                }
                scorePercentage = isCorrect ? 100 : 0;
                break;

            case FIXING_ANSWER:
                FixingKey readingKey = (FixingKey) key;
                String readingAnswer = submit.getFixingAnswer();
                if (!CoreUtils.isEmpty(readingAnswer)) {
                    isCorrect = normalizeText(readingAnswer)
                            .equals(normalizeText(readingKey.getCorrectText()));
                }
                scorePercentage = isCorrect ? 100 : 0;
                break;
            case TRACING:
                if (submit.getTracingResults() != null && !submit.getTracingResults().isEmpty()) {
                    long totalParts = submit.getTracingResults().size();
                    long correctParts = submit.getTracingResults().values().stream().filter(Boolean.TRUE::equals).count();
                    scorePercentage = (int) (((double) correctParts / totalParts) * 100);
                    isCorrect = (scorePercentage == 100);
                } else {
                    isCorrect = Boolean.TRUE.equals(submit.getIsFrontendSuccess());
                    scorePercentage = isCorrect ? 100 : 0;
                }
                break;
        }
        return new ResGradingResult(isCorrect, question.getCoinReward(), null,
                orderingFeedback, gapFeedback, matchingFeedback, scorePercentage);
    }

    public ResGradingResult gradeWriting(ExerciseQuestion question, ReqExerciseSubmit submit, Student student) {
        WritingContent content = (WritingContent) question.getContent();
        Integer minLimit = content.getMinWords();
        String text = submit.getWritingText();

        int wordCount = (text == null || text.isBlank()) ? 0 : text.trim().split("\\s+").length;

        if (minLimit != null && wordCount < minLimit) {
            throw new ValidationException(MessageKey.EXERCISE_WRITING_SHORT.getKey()
                    + minLimit + " words. You have: " + wordCount);
        }


        Optional<WritingSubmission> existingSub = writingSubmissionRepository
                .findByStudentUuidAndExerciseQuestionUuid(student.getUuid(), question.getUuid());

        Optional<ExerciseAnswers> existingAns = exerciseAnswersRepository
                .findByStudentAndExerciseQuestion(student, question);

        if (existingSub.isPresent()) {
            WritingSubmission sub = existingSub.get();

            if (sub.getStatus().equals(ExerciseSubmissionStatus.PENDING_REVIEW)) {
                throw new ValidationException(MessageKey.EXERCISE_PENDING_REVIEW.getKey());
            }

            if (existingAns.isPresent() && Boolean.TRUE.equals(existingAns.get().getIsCorrect())) {
                throw new ValidationException(MessageKey.EXERCISE_PASSED.getKey());
            }

            sub.setEssay(submit.getWritingText());
            sub.setStatus(ExerciseSubmissionStatus.PENDING_REVIEW);
            sub.setScore(0);
            writingSubmissionRepository.save(sub);
        } else {
            WritingSubmission writingSubmission = new WritingSubmission();
            writingSubmission.setUuid(UUID.randomUUID());
            writingSubmission.setEssay(submit.getWritingText());
            writingSubmission.setExerciseQuestion(question);
            writingSubmission.setStudent(student);
            writingSubmission.setStatus(ExerciseSubmissionStatus.PENDING_REVIEW);
            writingSubmissionRepository.save(writingSubmission);
        }


        ExerciseAnswers ans = existingAns.orElse(new ExerciseAnswers());
        if (ans.getId() == null) {
            ans.setStudent(student);
            ans.setExerciseQuestion(question);
            ExerciseTask task = taskRepo.findByUuid(submit.getTaskUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_TASK_NOT_FOUND.getKey()));
            ans.setExerciseTask(task);
        }

        try {
            ans.setAnswerContent(objectMapper.writeValueAsString(submit.getWritingText()));
        } catch (JsonProcessingException e) {
            ans.setAnswerContent("\"" + submit.getWritingText() + "\"");
        }
        ans.setIsCorrect(false);
        ans.setScore(0);
        exerciseAnswersRepository.save(ans);

        return new ResGradingResult(false, 0, "Sent for review!", null, null, null, 0);
    }

    private boolean gradeSelection(SelectionKey key, String userOptionId) {
        return key.getCorrectOptionId().equals(userOptionId);
    }

    private String normalizeAggressive(String value) {
        if (value == null) return "";

        String normalized = CoreUtils.normalizeCharacters(value);

        //
        return normalized.toLowerCase()
                .replace(".", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public List<Boolean> getGranularOrderingFeedback(OrderingKey key, List<String> userOrder) {
        List<String> correctOrder = key.getCorrectOrder();
        List<Boolean> feedback = new ArrayList<>();
        for (int i = 0; i < userOrder.size(); i++) {
            String normUser = normalizeAggressive(userOrder.get(i));
            String normCorrect = normalizeAggressive(correctOrder.get(i));
            feedback.add(normUser.equals(normCorrect));
        }
        return feedback;
    }

    public Map<String, Boolean> gradeMatching(MatchingKey key, Map<String, String> userPairs) {
        Map<String, Boolean> feedback = new HashMap<>();
        key.getPairs().forEach((left, right) -> {
            String userRight = userPairs.get(left);
            feedback.put(left, right.equals(userRight));
        });
        return feedback;
    }

    public boolean isGapCorrect(GapFillKey key, String gapId, String userValue) {
        List<String> correctVariants = key.getAnswers().get(gapId);
        if (correctVariants == null || userValue == null) return false;

        String normalizedUser = normalizeAggressive(userValue);

        return correctVariants.stream()
                .map(this::normalizeAggressive)
                .anyMatch(normalizedCorrect -> normalizedCorrect.equals(normalizedUser));
    }



    private Map<String, Boolean> gradeGapFill(GapFillKey key, Map<String, String> userAnswers) {
        Map<String, Boolean> feedback = new HashMap<>();
        key.getAnswers().keySet().forEach(gapId -> {
            boolean correct = isGapCorrect(key, gapId, userAnswers.get(gapId));
            feedback.put(gapId, correct);
        });
        return feedback;
    }

    private String normalizeText(String text) {
        return CoreUtils.normalizeCharacters(text)
                .replaceAll("\n", "")
                .trim();
    }
}