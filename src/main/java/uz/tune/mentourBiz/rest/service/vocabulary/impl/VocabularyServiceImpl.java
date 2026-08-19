package uz.tune.mentourBiz.rest.service.vocabulary.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.Region;
import uz.tune.mentourBiz.rest.domain.SchoolAcademicConfig;
import uz.tune.mentourBiz.rest.domain.SchoolSubscription;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularyQuestion;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularySet;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularyWord;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.postVocabulary.StudentWordMistake;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.postVocabulary.VocabularyAnswer;
import uz.tune.mentourBiz.rest.domain.shopManagement.coins.CoinTransaction;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.*;
import uz.tune.mentourBiz.rest.payload.res.exercise.ResVocabSet;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResVocabGradingResult;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResVocabLearnWord;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResVocabQuizWord;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResVocabSetResult;
import uz.tune.mentourBiz.rest.repository.SchoolAcademicConfigRepo;
import uz.tune.mentourBiz.rest.repository.SchoolSubscriptionRepo;
import uz.tune.mentourBiz.rest.repository.UnitExamSessionRepository;
import uz.tune.mentourBiz.rest.repository.schedule.GroupScheduleRepository;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.repository.transaction.CoinTransactionRepository;
import uz.tune.mentourBiz.rest.repository.unit.UnitRepository;
import uz.tune.mentourBiz.rest.repository.unit.VocabularyAnswerRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.VocabularyWordRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.vocabulary.StudentWordMistakeRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.vocabulary.VocabularyQuestionRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.vocabulary.VocabularySetRepository;
import uz.tune.mentourBiz.rest.service.AiExplanationService;
import uz.tune.mentourBiz.rest.service.ExamService;
import uz.tune.mentourBiz.rest.service.RewardCalculationService;
import uz.tune.mentourBiz.rest.service.exercise.impl.ProgressService;
import uz.tune.mentourBiz.rest.service.helper.AuthToViewEntity;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;
import uz.tune.mentourBiz.rest.service.vocabulary.VocabularyService;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VocabularyServiceImpl implements VocabularyService {

    private final VocabularySetRepository vocabularySetRepo;
    private final VocabularyWordRepository vocabularyWordRepo;
    private final VocabularyQuestionRepository vocabularyQuestionRepo;
    private final VocabularyAnswerRepository vocabularyAnswerRepo;
    private final StudentWordMistakeRepository mistakeRepo;
    private final UserService userService;
    private final StudentRepo studentRepo;
    private final CoinTransactionRepository coinTransactionRepo;
    private final ProgressService progressService;
    private final AuthToViewEntity authToViewEntity;
    private final UnitExamSessionRepository unitExamSessionRepository;
    private final ExamService examService;
    private final AiExplanationService aiExplanationService;
    private final UnitRepository unitRepository;
    private final SchoolSubscriptionRepo schoolSubscriptionRepo;
    private final SchoolAcademicConfigRepo schoolAcademicConfigRepo;
    private final GroupScheduleRepository groupScheduleRepository;
    private final UserScopeService userScopeService;
    private final RewardCalculationService rewardCalculationService;

    @Override
    public List<ResVocabSet> getVocabularySetsForUnit(UUID unitId) {
        User currentUser = userService.getCurrentUser();
        Unit unit = unitRepository.findByUuid(unitId).orElseThrow(() -> new EntityNotFoundException(MessageKey.UNIT_NOT_FOUND.getKey()));

        UUID schoolUuid = null;
        if (currentUser.getRole() == UserRole.STUDENT) {
            Student student = studentRepo.findByUser(currentUser).orElseThrow();

            if (unit.getUnitType() == UnitType.EXAM) {
                examService.validateAccess(student, unit, LessonSectionType.VOCABULARY);
            }

            schoolUuid = student.getSchool().getUuid();
        } else {
            schoolUuid = userScopeService.getAuthorizedSchoolUuids().stream().findFirst().orElse(null);
        }

        boolean aiEnabled = (schoolUuid != null) && schoolSubscriptionRepo.findBySchool_Uuid(schoolUuid)
                .map(SchoolSubscription::isAiExerciseEnabled).orElse(false);

        List<VocabularySet> sets = vocabularySetRepo.findAllByUnit_UuidAndStatusOrderBySortOrderAsc(unitId, VocabularySetStatus.ACTIVE);

        if (!currentUser.getRole().equals(UserRole.STUDENT)) {
            return sets.stream().map(st -> new ResVocabSet(st, 0, aiEnabled)).toList();
        }

        Student student = studentRepo.findByUser(currentUser).get();
        return sets.stream().map(set -> {
            List<VocabularyQuestion> qs = vocabularyQuestionRepo.findAllByVocabularySet_Uuid(set.getUuid());
            long totalPossible = qs.stream().mapToLong(q -> q.getScoreReward() != null ? q.getScoreReward() : 0L).sum();
            List<VocabularyAnswer> userAnswers = vocabularyAnswerRepo.findAllByStudentAndVocabularyQuestionInAndVocabularySet(student, qs, set);
            long totalEarned = userAnswers.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).mapToLong(a -> a.getScore() != null ? a.getScore() : 0L).sum();

            int percentage = totalPossible > 0 ? (int) ((totalEarned * 100.0) / totalPossible) : 0;
            ResVocabSet dto = new ResVocabSet(set, percentage, aiEnabled);
            dto.setAnsweredAll(!qs.isEmpty() && userAnswers.size() >= qs.size());
            return dto;
        }).collect(Collectors.toList());
    }
    @Override
    @Transactional(readOnly = true)
    public List<ResVocabQuizWord> getQuizWordsForSet(UUID setUuid) {
        User currentUser = userService.getCurrentUser();
        VocabularySet vocabularySet = vocabularySetRepo.findByUuid(setUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.VOCAB_SET_NOT_FOUND.getKey()));


        if (vocabularySet.getUnit().getUnitType() == UnitType.EXAM && currentUser.getRole() == UserRole.STUDENT) {
            Student student = studentRepo.findByUser(currentUser).orElseThrow();
            examService.validateAccess(student, vocabularySet.getUnit(), LessonSectionType.VOCABULARY);
        }

        List<VocabularyQuestion> allQuestions = vocabularyQuestionRepo.findAllByVocabularySet_Uuid(setUuid);



        Region region = null;
        if (currentUser.getRole().equals(UserRole.STUDENT)) {
            region = studentRepo.findByUser(currentUser).map(s -> s.getSchool().getRegion()).orElse(null);
        }

        if (!currentUser.getRole().equals(UserRole.STUDENT)) {
            final Region finalRegion = region;
            return allQuestions.stream()
                    .map(q -> new ResVocabQuizWord(q.getVocabularyWord(), setUuid, finalRegion))
                    .toList();
        }

        Student student = studentRepo.findByUser(currentUser).orElseThrow();
        List<VocabularyAnswer> answers = vocabularyAnswerRepo.
                findAllByStudentAndVocabularyQuestionInAndVocabularySet(student, allQuestions, vocabularySet);
        Set<UUID> answeredIds = null;

        if (vocabularySet.getUnit().getUnitType() == UnitType.EXAM) {
            answeredIds = answers.stream()
                    .map(a -> a.getVocabularyQuestion().getUuid())
                    .collect(Collectors.toSet());
        }
        else {
            answeredIds = answers.stream()
                    .filter(a -> Boolean.TRUE.equals(a.getIsCorrect()))
                    .map(a -> a.getVocabularyQuestion().getUuid())
                    .collect(Collectors.toSet());
        }

        final Region finalRegion = region;
        Set<UUID> finalAnsweredIds = answeredIds;
        return allQuestions.stream()
                .filter(q -> !finalAnsweredIds.contains(q.getUuid()))
                .map(q -> new ResVocabQuizWord(q.getVocabularyWord(), setUuid, finalRegion))
                .collect(Collectors.toList());
    }
    @Override
    @Transactional(readOnly = true)
    public List<ResVocabLearnWord> getLearnWordsForSet(UUID setUuid) {
        VocabularySet set = vocabularySetRepo.findByUuid(setUuid).orElseThrow();
        User user = userService.getCurrentUser();
        List<VocabularyQuestion> questions = vocabularyQuestionRepo.findAllByVocabularySet_Uuid(setUuid);

        Region region = null;
        if (user.getRole().equals(UserRole.STUDENT)) {
            Student student = studentRepo.findByUser(user).orElseThrow();
            region = student.getSchool().getRegion();

            if (set.getUnit().getUnitType() == UnitType.EXAM) {
                examService.validateAccess(student, set.getUnit(), LessonSectionType.VOCABULARY);

                List<VocabularyAnswer> answers = vocabularyAnswerRepo.findAllByStudentAndVocabularyQuestionInAndVocabularySet(student, questions, set);
                Set<UUID> answeredIds = answers.stream()
                        .map(a -> a.getVocabularyQuestion().getUuid())
                        .collect(Collectors.toSet());

                questions = questions.stream()
                        .filter(q -> !answeredIds.contains(q.getUuid()))
                        .toList();
            }
        }

        final Region finalRegion = region;
        return questions.stream()
                .map(q -> new ResVocabLearnWord(q.getVocabularyWord(), q, finalRegion))
                .collect(Collectors.toList());
    }


    private String normalizeAggressive(String value) {
        if (value == null) return "";

        return value.toLowerCase()
                .replace(".", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    @Override
    @Transactional
    public ResVocabGradingResult gradeVocabulary(UUID wordUuid, String userTranslation, UUID setUuid) {
        User currentUser = userService.getCurrentUser();
        VocabularyWord word = vocabularyWordRepo.findByUuid(wordUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.WORD_NOT_FOUND.getKey()));
        VocabularyQuestion question = vocabularyQuestionRepo.findByVocabularyWordAndVocabularySetUuid(word, setUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_QUESTION_NOT_FOUND.getKey()));

        if (question.getVocabularySet().getUnit().getSchoolBook() != null) {
            groupScheduleRepository.findByGroupUuidAndLessonContainingUnit(
                    null, question.getVocabularySet().getUnit().getUuid(), GroupScheduleStatus.ACTIVE
            ).ifPresent(schedule -> {
                if (schedule.getLesson().getCourse().getStatus() == CourseStatus.FINISHED) {
                    throw new ValidationException(MessageKey.COURSE_FINISHED.getKey());
                }
            });
        }

        if (question.getVocabularySet().getUnit().getUnitType() == UnitType.EXAM && currentUser.getRole() == UserRole.STUDENT) {
            Student student = studentRepo.findByUser(currentUser).orElseThrow();
            examService.validateAccess(student, question.getVocabularySet().getUnit(), LessonSectionType.VOCABULARY);
        }

        Student student = studentRepo.findByUser(currentUser)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

        String normUser = normalizeAggressive(userTranslation);
        String normCorrect = normalizeAggressive(word.getWord());


        boolean isCorrect = normUser.equals(normCorrect);

        int calculatedReward = rewardCalculationService.getDynamicVocabReward(
                student.getSchool().getUuid(),
                question.getCoinReward() != null ? question.getCoinReward() : 2
        );

        if (!currentUser.getRole().equals(UserRole.STUDENT)) {
            return new ResVocabGradingResult(isCorrect, 0, isCorrect ? "Correct" : "Incorrect");
        }

        VocabularyAnswer answer = vocabularyAnswerRepo.findByStudentAndVocabularyQuestion(student, question)
                .orElse(new VocabularyAnswer());

        boolean wasAlreadyCorrect = Boolean.TRUE.equals(answer.getIsCorrect());

        SchoolAcademicConfig config = schoolAcademicConfigRepo.findBySchool_Uuid(student.getSchool().getUuid())
                .orElse(new SchoolAcademicConfig());

        if (config.isPenaltyEnabled() && answer.getId() != null && !wasAlreadyCorrect) {
            if (answer.getAttemptCount() != null && answer.getAttemptCount() >= config.getMaxRetries()) {
                throw new ValidationException(MessageKey.EXERCISE_MAX_RETRIES.getKey());
            }
        }

        if(answer.getId() == null) {
            answer.setStudent(student);
            answer.setVocabularyQuestion(question);
            answer.setVocabularySet(question.getVocabularySet());
            answer.setAttemptCount(0);
        }
        int count = (answer.getAttemptCount() != null) ? answer.getAttemptCount() : 0;
        answer.setAttemptCount(count + 1);
        answer.setAnswerContent(userTranslation);
        answer.setIsCorrect(isCorrect);

        int weightedScore = isCorrect ? calculatedReward : 0;
        answer.setScore(weightedScore);
        answer.setIsManually(false);
        answer.setUpdatedBy(null);

        vocabularyAnswerRepo.save(answer);

        int coins = 0;
        if (!isCorrect) {
            if (config.isPenaltyEnabled()) {
                student.setCoins(student.getCoins() - config.getPenaltyPerAttempt());
                studentRepo.save(student);
            }
            trackMistake(student, word);
        } else if (!wasAlreadyCorrect) {
            coins = calculatedReward;
            awardCoins(student, coins, weightedScore, "Vocabulary: " + word.getWord());
        }

        progressService.updateUnitProgress(student, question.getVocabularySet().getUnit().getUuid());
        return new ResVocabGradingResult(isCorrect, coins, isCorrect ? "Correct" : "Incorrect");
    }

    private void awardCoins(Student student, Integer coinAmount, Integer scoreAmount, String reason) {
        student.setCoins(student.getCoins() + coinAmount);
        student.setLifeTimeCoinBalance(student.getLifeTimeCoinBalance() + coinAmount);
        student.setScore(student.getScore() + scoreAmount);
        studentRepo.save(student);

        CoinTransaction tx = new CoinTransaction();
        tx.setStudent(student);
        tx.setAmount(coinAmount);
        tx.setType(TransactionType.EXERCISE_AWARD);
        tx.setReason(reason);
        coinTransactionRepo.save(tx);
    }

//    @Override
//    @Transactional(readOnly = true)
//    public List<ResVocabLearnWord> getLearnWordsForSet(UUID setUuid) {
//        VocabularySet set = vocabularySetRepo.findByUuid(setUuid).orElseThrow();
//        User currentUser = userService.getCurrentUser();
//
//        if (set.getUnit().getUnitType() == UnitType.EXAM && currentUser.getRole() == UserRole.STUDENT) {
//            Student student = studentRepo.findByUser(currentUser).orElseThrow();
//            if (!unitExamSessionRepository.existsByStudentAndUnit(student, set.getUnit())) {
//                throw new PermissionForbidden("Start the exam first!");
//            }
//        }
//
//        return vocabularyQuestionRepo.findAllByVocabularySet_Uuid(setUuid).stream()
//                .map(q -> new ResVocabLearnWord(q.getVocabularyWord()))
//                .collect(Collectors.toList());
//    }






    @Override
    @Transactional(readOnly = true)
    public ResVocabSetResult getVocabularySetResult(UUID setUuid, Boolean flag) {
        flag = flag!=null ? flag : false;
        User currentUser = userService.getCurrentUser();
        Student student = studentRepo.findByUser(currentUser)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

        return getVocabResultInternal(student, setUuid, flag);
    }

    @Override
    @Transactional(readOnly = true)
    public ResVocabSetResult getVocabResultForStudent(UUID studentUuid, UUID setUuid, Boolean flag) {
        flag = flag!=null ? flag : false;
        Student student = studentRepo.findByUserUuid(studentUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

        authToViewEntity.authorizeActionUponStudent(student);

        return getVocabResultInternal(student, setUuid, flag);
    }

    private ResVocabSetResult getVocabResultInternal(Student student, UUID setUuid, Boolean flag) {
        VocabularySet set = vocabularySetRepo.findByUuid(setUuid).orElseThrow();
        List<VocabularyQuestion> questions = vocabularyQuestionRepo.findAllByVocabularySet_Uuid(set.getUuid());
        List<VocabularyAnswer> answers = vocabularyAnswerRepo.findAllByStudentAndVocabularyQuestionInAndVocabularySet(student, questions, set);
        Map<UUID, VocabularyAnswer> answerMap = answers.stream().collect(Collectors.toMap(a -> a.getVocabularyQuestion().getUuid(), a -> a));

        int correctCount = 0, coinsEarned = 0, totalScoreEarned = 0;

        int vocabReward = rewardCalculationService.getDynamicVocabReward(student.getSchool().getUuid(), 2);
        long totalPossibleScore = (long) questions.size() * vocabReward;

        List<ResVocabSetResult.WordResultSummary> wordSummaries = new ArrayList<>();
        for (VocabularyQuestion q : questions) {
            VocabularyAnswer ans = answerMap.get(q.getUuid());
            boolean isCorrect = ans != null && Boolean.TRUE.equals(ans.getIsCorrect());
            if (ans != null && isCorrect) {
                correctCount++;
                totalScoreEarned += (ans.getScore() != null ? ans.getScore() : 0);
                coinsEarned += vocabReward;
            }
            wordSummaries.add(new ResVocabSetResult.WordResultSummary(
                    q.getVocabularyWord().getUuid(),
                    null,
                    q.getVocabularyWord().getWord(),
                    vocabReward,
                    vocabReward,
                    isCorrect,
                    ans != null ? ans.getAnswerContent() : null,
                    ans != null ? ans.getErrorExplanation() : null
            ));
        }

        int percentage = totalPossibleScore > 0 ? (int) (((double) totalScoreEarned / totalPossibleScore) * 100) : 0;
        return new ResVocabSetResult(set.getUuid(), set.getTitle(), questions.size(), correctCount, coinsEarned, totalScoreEarned, Math.min(percentage, 100), true, wordSummaries);
    }

    private void trackMistake(Student student, VocabularyWord word) {
        StudentWordMistake mistake = mistakeRepo.findByStudentAndVocabularyWord(student, word)
                .orElse(new StudentWordMistake());
        if (mistake.getId() == null) {
            mistake.setStudent(student);
            mistake.setVocabularyWord(word);
            mistake.setMistakeCount(0);
        }
        mistake.setMistakeCount(mistake.getMistakeCount() + 1);
        mistakeRepo.save(mistake);
    }

    private void awardCoins(Student student, Integer amount, String reason) {
        student.setCoins(student.getCoins() + amount);
        student.setLifeTimeCoinBalance(student.getLifeTimeCoinBalance() + amount);
        studentRepo.save(student);

        CoinTransaction tx = new CoinTransaction();
        tx.setStudent(student);
        tx.setAmount(amount);
        tx.setType(TransactionType.EXERCISE_AWARD);
        tx.setReason(reason);
        coinTransactionRepo.save(tx);
    }
}