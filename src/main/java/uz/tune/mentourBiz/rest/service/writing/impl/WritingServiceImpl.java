package uz.tune.mentourBiz.rest.service.writing.impl;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.config.GlobalVar;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.SchoolAcademicConfig;
import uz.tune.mentourBiz.rest.domain.SchoolSubscription;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseQuestion;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.postExercise.ExerciseAnswers;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.writing.WritingSubmission;
import uz.tune.mentourBiz.rest.domain.shopManagement.coins.CoinTransaction;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.AiStatus;
import uz.tune.mentourBiz.rest.enums.ExerciseSubmissionStatus;
import uz.tune.mentourBiz.rest.enums.TransactionType;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.payload.req.writing.ReqTeacherFeedBackForWriting;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.exercise.ResWriting;
import uz.tune.mentourBiz.rest.payload.res.lesson.ResWritingList;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResExerciseQuestion;
import uz.tune.mentourBiz.rest.repository.SchoolAcademicConfigRepo;
import uz.tune.mentourBiz.rest.repository.SchoolSubscriptionRepo;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentRepository;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.repository.transaction.CoinTransactionRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseAnswersRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseQuestionRepository;
import uz.tune.mentourBiz.rest.repository.user.TeacherRepository;
import uz.tune.mentourBiz.rest.repository.writing.WritingSubmissionRepository;
import uz.tune.mentourBiz.rest.service.AiExplanationService;
import uz.tune.mentourBiz.rest.service.RewardCalculationService;
import uz.tune.mentourBiz.rest.service.exercise.impl.ProgressService;
import uz.tune.mentourBiz.rest.service.helper.AuthToViewEntity;
import uz.tune.mentourBiz.rest.service.user.TeacherService;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;
import uz.tune.mentourBiz.rest.service.writing.WritingService;

import java.util.Collection;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WritingServiceImpl implements WritingService {

    private final ExerciseQuestionRepository exerciseQuestionRepository;
    private final WritingSubmissionRepository writingSubmissionRepository;
    private final UserService userService;
    private final UserScopeService userScopeService;
    private final ExerciseAnswersRepository exerciseAnswersRepository;
    private final StudentRepo studentRepo;
    private final AuthToViewEntity authToViewEntity;
    private final ProgressService progressService;
    private final TeacherService teacherService;
    private final EnrollmentRepository enrollmentRepository;
    private final TeacherRepository teacherRepository;
    private final SchoolAcademicConfigRepo schoolAcademicConfigRepo;
    private final SchoolSubscriptionRepo schoolSubscriptionRepo;
    private final AiExplanationService aiExplanationService;
    private final CoinTransactionRepository coinTransactionRepository;
    private final RewardCalculationService rewardCalculationService;


    @Override
    public ResWriting getStudentsWriting(UUID studentId, UUID questionUuid) {
        User currentUser = userService.getCurrentUser();
        Student student = studentRepo.findByUuid(studentId).orElseThrow();

        if (!currentUser.getRole().equals(UserRole.SYS_ADMIN)) {
            authToViewEntity.authorizeActionUponStudent(student);
        }

        WritingSubmission sub = writingSubmissionRepository
                .findByStudentUuidAndExerciseQuestionUuid(student.getUuid(), questionUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_SUBMISSION_NOT_FOUND.getKey()));

        return new ResWriting(new ResExerciseQuestion(sub.getExerciseQuestion()), sub.getEssay(), sub.getFeedback(), sub);
    }

    @Override
    public ResWriting getWritingSubmission(UUID submissionUuid) {
        User currentUser = userService.getCurrentUser();

        WritingSubmission writingSubmission = writingSubmissionRepository.findByUuid(submissionUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_SUBMISSION_NOT_FOUND.getKey()));

        Student student = writingSubmission.getStudent();
        ExerciseQuestion exerciseQuestion = writingSubmission.getExerciseQuestion();

        if (currentUser.getRole().equals(UserRole.STUDENT)) {
            if (!student.getUser().getUuid().equals(currentUser.getUuid())) {
                throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
            }
        } else if (currentUser.getRole().equals(UserRole.TEACHER)) {
            boolean isMyStudent = enrollmentRepository.existsByStudentAndGroup_Teacher_User_Uuid(
                    student, currentUser.getUuid());

            if (!isMyStudent) {
                throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
            }
        } else if (!currentUser.getRole().equals(UserRole.SYS_ADMIN)) {
            authToViewEntity.authorizeActionUponStudent(student);
        }

        return new ResWriting(
                new ResExerciseQuestion(exerciseQuestion),
                writingSubmission.getEssay(),
                writingSubmission.getFeedback(),
                writingSubmission
        );
    }

    @Override
    @Transactional
    public ResponseMessage triggerAiReview(UUID submissionUuid) {
        WritingSubmission submission = writingSubmissionRepository.findByUuid(submissionUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_SUBMISSION_NOT_FOUND.getKey()));

        authToViewEntity.authorizeActionUponStudent(submission.getStudent());

        SchoolSubscription sub = schoolSubscriptionRepo.findBySchool_Uuid(submission.getStudent().getSchool().getUuid())
                .orElseThrow(() -> new ValidationException(MessageKey.SUBSCRIPTION_EXPIRED.getKey()));

        if (!sub.isAiWritingEnabled()) {
            throw new PermissionForbidden(MessageKey.AI_DISABLED.getKey());
        }

        aiExplanationService.processWritingAiAsync(
                submissionUuid,
                GlobalVar.getLang(),
                GlobalVar.getUsername(),
                GlobalVar.getRequestId()
        );

        return new ResponseMessage("AI Review requested.");
    }

    @Override
    @Transactional
    public ResponseMessage rejectSubmission(UUID submissionUuid) {
        WritingSubmission submission = writingSubmissionRepository.findByUuid(submissionUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_SUBMISSION_NOT_FOUND.getKey()));

        // Check if user has permission for this student
        authToViewEntity.authorizeActionUponStudent(submission.getStudent());

        // 1. Remove from ExerciseAnswers (so the app doesn't show it as 'Passed')
        exerciseAnswersRepository.findByStudentAndExerciseQuestion(submission.getStudent(), submission.getExerciseQuestion())
                .ifPresent(exerciseAnswersRepository::delete);

        // 2. Delete the specific WritingSubmission
        writingSubmissionRepository.delete(submission);

        // 3. Recalculate progress for the student
        progressService.updateUnitProgress(submission.getStudent(),
                submission.getExerciseQuestion().getExerciseTask().get(0).getUnit().getUuid());

        return new ResponseMessage("Writing submission rejected. Student can now resubmit.");
    }

    @Transactional
    public void processAiResult(UUID submissionUuid, Integer score, String feedback, Integer grammar, Integer vocab, Integer coherence) {
        WritingSubmission sub = writingSubmissionRepository.findByUuid(submissionUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_SUBMISSION_NOT_FOUND.getKey()));

        sub.setScore(score);
        sub.setFeedback(feedback);
        sub.setGrammarScore(grammar);
        sub.setVocabularyScore(vocab);
        sub.setCoherenceScore(coherence);
        sub.setStatus(ExerciseSubmissionStatus.GRADED);
        sub.setAiStatus(AiStatus.COMPLETED);
        writingSubmissionRepository.save(sub);

        Student student = sub.getStudent();
        SchoolAcademicConfig config = schoolAcademicConfigRepo.findBySchool_Uuid(student.getSchool().getUuid())
                .orElse(new SchoolAcademicConfig());

        ExerciseAnswers ans = exerciseAnswersRepository.findByStudentAndExerciseQuestion(student, sub.getExerciseQuestion())
                .orElse(new ExerciseAnswers());

        if (ans.getId() == null) {
            ans.setStudent(student);
            ans.setExerciseQuestion(sub.getExerciseQuestion());
            if (!sub.getExerciseQuestion().getExerciseTask().isEmpty()) {
                ans.setExerciseTask(sub.getExerciseQuestion().getExerciseTask().get(0));
            }
        }

        boolean passed = score >= config.getMinScoreToPass();
        ans.setIsCorrect(passed);

        // Use the dynamic reward (same base the WRITING section progress denominator uses),
        // otherwise the earned score and the totalPossible are computed on different scales.
        int possibleReward = rewardCalculationService.getDynamicExerciseReward(sub.getExerciseQuestion(), student.getSchool().getUuid());
        int weightedScore = (int) ((score / 100.0) * possibleReward);
        ans.setScore(weightedScore);

        if (passed && !Boolean.TRUE.equals(ans.getEverCorrect())) {
            ans.setEverCorrect(true);
            int coins = sub.getExerciseQuestion().getCoinReward();

            student.setCoins(student.getCoins() + coins);
            student.setLifeTimeCoinBalance(student.getLifeTimeCoinBalance() + coins);
            student.setScore(student.getScore() + weightedScore);
            studentRepo.save(student);

            CoinTransaction tx = new CoinTransaction();
            tx.setStudent(student);
            tx.setAmount(coins);
            tx.setType(TransactionType.EXERCISE_AWARD);
            tx.setReason("AI Graded Writing");
            coinTransactionRepository.save(tx);
        }

        exerciseAnswersRepository.save(ans);
        if (ans.getExerciseTask() != null) {
            progressService.updateUnitProgress(student, ans.getExerciseTask().getUnit().getUuid());
        }
    }

    @Override
    @Transactional
    public ResponseMessage gradeAndGiveFeedback(UUID submissionUUID, ReqTeacherFeedBackForWriting req) {
        if (req.getScore() > 100 || req.getScore() < 0) {
            throw new ValidationException(MessageKey.SCORE_OUT_OF_RANGE.getKey());
        }

        WritingSubmission submission = writingSubmissionRepository.findByUuid(submissionUUID)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_SUBMISSION_NOT_FOUND.getKey()));

        User currentUser = userService.getCurrentUser();
        Student student = submission.getStudent();

        authToViewEntity.authorizeActionUponStudent(student);

        submission.setFeedback(req.getFeedback());
        submission.setScore(req.getScore());
        submission.setCoinsAwarded(req.getCoins());
        submission.setGrammarScore(req.getGrammarScore() != null ? req.getGrammarScore() : 0);
        submission.setVocabularyScore(req.getVocabularyScore() != null ? req.getVocabularyScore() : 0);
        submission.setCoherenceScore(req.getCoherenceScore() != null ? req.getCoherenceScore() : 0);
        submission.setStatus(ExerciseSubmissionStatus.GRADED);

        teacherRepository.findByUser_Uuid(currentUser.getUuid()).ifPresent(submission::setTeacher);
        writingSubmissionRepository.save(submission);

        ExerciseAnswers ans = exerciseAnswersRepository
                .findByStudentAndExerciseQuestion(student, submission.getExerciseQuestion())
                .orElse(new ExerciseAnswers());

        if (ans.getId() == null) {
            ans.setStudent(student);
            ans.setExerciseQuestion(submission.getExerciseQuestion());
            if (!submission.getExerciseQuestion().getExerciseTask().isEmpty()) {
                ans.setExerciseTask(submission.getExerciseQuestion().getExerciseTask().get(0));
            }
        }

        // Align with the WRITING section progress denominator (dynamic reward base).
        int possibleReward = rewardCalculationService.getDynamicExerciseReward(submission.getExerciseQuestion(), student.getSchool().getUuid());
        int weightedScore = (int) ((req.getScore() / 100.0) * possibleReward);
        ans.setScore(weightedScore);

        SchoolAcademicConfig config = schoolAcademicConfigRepo.findBySchool_Uuid(student.getSchool().getUuid())
                .orElse(new SchoolAcademicConfig());

        boolean passed = req.getScore() >= config.getMinScoreToPass();
        ans.setIsCorrect(passed);

        if (passed && !Boolean.TRUE.equals(ans.getEverCorrect())) {
            ans.setEverCorrect(true);

            int dynamicFallback = rewardCalculationService.getDynamicExerciseReward(
                    submission.getExerciseQuestion(), student.getSchool().getUuid());

            Integer coinsToAward = (req.getCoins() != null) ? req.getCoins() : dynamicFallback;

            student.setCoins(student.getCoins() + coinsToAward);
            student.setLifeTimeCoinBalance(student.getLifeTimeCoinBalance() + (long) coinsToAward);
            student.setScore(student.getScore() + weightedScore);
            studentRepo.save(student);

            CoinTransaction tx = new CoinTransaction();
            tx.setStudent(student);
            tx.setAmount(coinsToAward);
            tx.setType(TransactionType.ADMINISTRATION_AWARD);
            tx.setReason("Writing Graded: " + submission.getExerciseQuestion().getInstruction());
            tx.setGivenBy(currentUser.getUsername());
            coinTransactionRepository.save(tx);
        }

        exerciseAnswersRepository.save(ans);

        if (ans.getExerciseTask() != null && ans.getExerciseTask().getUnit() != null) {
            progressService.updateUnitProgress(student, ans.getExerciseTask().getUnit().getUuid());
        }

        return new ResponseMessage(passed ? "Essay passed and graded!" : "Grade submitted. Student needs to retry.");
    }
    @Override
    @Transactional(readOnly = true)
    public Page<ResWritingList> getAllSubmissions(ExerciseSubmissionStatus status, UUID studentUuid, UUID groupUuid, Pageable pageable) {
        User currentUser = userService.getCurrentUser();

        Collection<UUID> schoolUuids = userScopeService.getAuthorizedSchoolUuids();

        UUID teacherUuid = currentUser.getRole().equals(UserRole.TEACHER) ? currentUser.getUuid() : null;

        return writingSubmissionRepository.findAllFilteredMulti(
                schoolUuids,
                teacherUuid,
                status,
                studentUuid,
                groupUuid,
                pageable
        ).map(ResWritingList::new);
    }
}
