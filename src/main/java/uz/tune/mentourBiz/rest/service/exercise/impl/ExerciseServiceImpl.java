package uz.tune.mentourBiz.rest.service.exercise.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.config.GlobalVar;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.CharacterSvg;
import uz.tune.mentourBiz.rest.domain.SchoolAcademicConfig;
import uz.tune.mentourBiz.rest.domain.SchoolExamSettings;
import uz.tune.mentourBiz.rest.domain.SchoolSubscription;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.SchoolBook;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.speaking.AiResponse;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.speaking.SpeakingSubmission;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseQuestion;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseTask;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.postExercise.ExerciseAnswers;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularyQuestion;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularySet;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.postVocabulary.VocabularyAnswer;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.writing.WritingSubmission;
import uz.tune.mentourBiz.rest.domain.shopManagement.coins.CoinTransaction;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.*;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.PronunciationContent;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.answers.typesOfAnswes.GapFillKey;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.answers.typesOfAnswes.MatchingKey;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.answers.typesOfAnswes.OrderingKey;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.answers.typesOfAnswes.MultiSelectKey;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.answers.typesOfAnswes.SelectionKey;
import uz.tune.mentourBiz.rest.payload.questionFormats.types.nodes.CircleTracingContent;
import uz.tune.mentourBiz.rest.payload.req.exercise.ReqManualGrade;
import uz.tune.mentourBiz.rest.payload.res.ResSpeaking;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.attempt.ResAttemptStatus;
import uz.tune.mentourBiz.rest.payload.res.attempt.SchoolLimitAttempts;
import uz.tune.mentourBiz.rest.payload.res.exercise.ResExerciseSection;
import uz.tune.mentourBiz.rest.payload.res.exercise.ResWriting;
import uz.tune.mentourBiz.rest.payload.studentReq.req.exercise.ReqExerciseSubmit;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.*;
import uz.tune.mentourBiz.rest.repository.*;
import uz.tune.mentourBiz.rest.repository.schedule.GroupScheduleRepository;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.repository.transaction.CoinTransactionRepository;
import uz.tune.mentourBiz.rest.repository.unit.UnitRepository;
import uz.tune.mentourBiz.rest.repository.unit.VocabularyAnswerRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseAnswersRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseQuestionRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseTaskRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.vocabulary.VocabularyQuestionRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.vocabulary.VocabularySetRepository;
import uz.tune.mentourBiz.rest.repository.user.UserRepo;
import uz.tune.mentourBiz.rest.repository.writing.WritingSubmissionRepository;
import uz.tune.mentourBiz.rest.service.AiExplanationService;
import uz.tune.mentourBiz.rest.service.ExamService;
import uz.tune.mentourBiz.rest.service.RewardCalculationService;
import uz.tune.mentourBiz.rest.service.exercise.ExerciseGradingService;
import uz.tune.mentourBiz.rest.service.exercise.ExerciseService;
import uz.tune.mentourBiz.rest.service.helper.AuthToViewEntity;
import uz.tune.mentourBiz.rest.service.school.SchoolAttemptService;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;
import uz.tune.mentourBiz.utils.CoreUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExerciseServiceImpl implements ExerciseService {
    private final ExerciseQuestionRepository questionRepo;
    private final ExerciseGradingService gradingService;
    private final UserService userService;
    private final StudentRepo studentRepo;
    private final ExerciseAnswersRepository answerRepo;
    private final CoinTransactionRepository coinTransactionRepo;
    private final ObjectMapper objectMapper;
    private final ExerciseTaskRepository taskRepo;
    private final ProgressService progressService;
    private final VocabularySetRepository vocabSetRepo;
    private final VocabularyAnswerRepository vocabAnswerRepo;
    private final AuthToViewEntity authToViewEntity;
    private final WritingSubmissionRepository writingSubmissionRepository;
    private final ExerciseQuestionRepository exerciseQuestionRepository;
    private final SpeakingSubmissionRepository speakingRepo;
    private final AiResponseRepository aiResponseRepository;
    private final ExamService examService;
    private final UnitRepository unitRepository;
    private final UnitExamSessionRepository unitExamSessionRepository;
    private final AiExplanationService aiExplanationService;
    private final SchoolAcademicConfigRepo schoolAcademicConfigRepo;
    private final SchoolSubscriptionRepo schoolSubscriptionRepo;
    private final VocabularyQuestionRepository vocabularyQuestionRepository;
    private final SchoolExamSettingsRepo schoolExamSettingsRepo;
    private final GroupScheduleRepository groupScheduleRepository;
    private final CharacterSvgRepository characterSvgRepository;
    private final UserScopeService userScopeService;
    private final SchoolDirectorRepo schoolDirectorRepo;
    private final RewardCalculationService rewardCalculationService;
    private final uz.tune.mentourBiz.rest.repository.unit.exercise.TaskAttemptRepository taskAttemptRepository;
    private final SchoolAttemptService schoolAttemptService;
    private final UserRepo userRepo;

    @Override
    public ResUnitTaskInfos getExerciseForUnit(UUID unitId, LessonSectionType type) {
        User currentUser = userService.getCurrentUser();
        Student student = studentRepo.findByUser(currentUser)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

        Unit unit = unitRepository.findByUuid(unitId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.UNIT_NOT_FOUND.getKey()));

        if (unit.getUnitType() == UnitType.EXAM) {
            examService.validateAccess(student, unit, type);
        }

        return getExerciseForUnitInternal(student, unitId, type);
    }


    @Override
    @Transactional(readOnly = true)
    public ResUnitTaskInfos getExercisesForStudent(UUID userUuid, UUID unitId, LessonSectionType type) {
        System.out.println("getExercisesForStudent + "+userUuid+"+unitId");
        Optional<User> user = userRepo.findByUuid(userUuid);
        if (user.isEmpty()) {
            System.out.println("User not found : " + userUuid);
            throw new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey());
        }
        User user1 = user.get();

        Optional<Student> student1 = studentRepo.findByUser(user1);
        if (student1.isEmpty()) {
            System.out.println("Student  not found : " + user1.getUuid());
            throw new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey());
        }
        System.out.println("Student found : " + student1.get().getUuid());
        Student student = student1.get();


        authToViewEntity.authorizeActionUponStudent(student);
        System.out.println("Student Authorized Action Upon Student : " + student.getUuid());
        if (type != null) {
            return getExerciseForUnitInternal(student, unitId, type);
        }

        List<ResExerciseSection> allSections = new ArrayList<>();

        for (LessonSectionType sectionType : LessonSectionType.values()) {

            System.out.println("Processing section type: " + sectionType);
            ResUnitTaskInfos sectionResult = getExerciseForUnitInternal(student, unitId, sectionType);

            if (!sectionResult.getSections().isEmpty()) {
                System.out.println("Adding sections for type: " + sectionType + ", number of sections: " + sectionResult.getSections().size());
                allSections.addAll(sectionResult.getSections());
            }
        }

        return new ResUnitTaskInfos(allSections);
    }

    private ResUnitTaskInfos getExerciseForUnitInternal(Student student, UUID unitId, LessonSectionType type) {
        System.out.println("getExerciseForUnit + "+student.getUuid()+"+unitId");
        Unit unit = unitRepository.findByUuid(unitId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.UNIT_NOT_FOUND.getKey()));
        if (unit.getUnitType() == UnitType.EXAM) {
            boolean sessionExists = unitExamSessionRepository.existsByStudentAndUnit(student, unit);
            if (!sessionExists) {
                throw new PermissionForbidden(MessageKey.EXAM_SESSION_NOT_FOUND.getKey());
            }
        }

        if (type == null) {
            throw new ValidationException(MessageKey.SECTION_TYPE_REQUIRED.getKey());
        }

        List<ResUnitTaskInfo> taskDtos = switch (type) {
            case VOCABULARY -> getVocabTasks(student, unitId);
            case WRITING -> getWritingTasks(student, unitId);
            case SPEAKING -> getSpeakingTasks(student, unitId);
            case GRAMMAR, LISTENING, READING -> getStandardExerciseTasks(student, unitId, type);
        };

        List<ResExerciseSection> responseSections = new ArrayList<>();
        if (!taskDtos.isEmpty()) {
            System.out.println("Adding section for type: " + type + ", number of tasks: " + taskDtos.size());
            taskDtos.sort(Comparator.comparing(ResUnitTaskInfo::getSortOrder));
            responseSections.add(new ResExerciseSection(type, taskDtos));
        }
        System.out.println("Returning sections for unit: " + unitId + ", number of sections: " + responseSections.size());
        return new ResUnitTaskInfos(responseSections);
    }

    private List<ResUnitTaskInfo> getSpeakingTasks(Student student, UUID unitId) {
        List<ExerciseTask> tasks = taskRepo.findAllByUnit_UuidAndSectionTypeOrderBySortOrderAsc(unitId, LessonSectionType.SPEAKING);
        if (!userService.getCurrentUser().getRole().equals(UserRole.SYS_ADMIN)) {
            tasks = tasks.stream().filter(t -> t.getStatus() == ExerciseTaskStatus.ACTIVE).toList();
        }
        return tasks.stream().map(task -> {
            List<ExerciseQuestion> questions = questionRepo.findAllByExerciseTask_UuidAndType(task.getUuid(), ExerciseType.SPEAKING);
            int submissionsCount = 0;
            List<ResSpeaking> speakingData = new ArrayList<>();

            long totalPossible = questions.stream().mapToLong(q -> q.getScoreReward() != null ? q.getScoreReward() : 0L).sum();
            long totalEarned = 0;

            for (ExerciseQuestion q : questions) {
                Optional<SpeakingSubmission> subOpt = speakingRepo.findByStudent_User_UuidAndExerciseQuestion_Uuid(student.getUser().getUuid(), q.getUuid());
                if (subOpt.isPresent()) {
                    submissionsCount++;
                    SpeakingSubmission sub = subOpt.get();
                    if (sub.getScores() != null) {
                        int possible = q.getScoreReward() != null ? q.getScoreReward() : 0;
                        totalEarned += (long) ((sub.getScores().getOverallScore() / 100.0) * possible);
                    }
                }
                AiResponse aiResponse = subOpt.flatMap(aiResponseRepository::findBySpeakingSubmission).orElse(null);
                speakingData.add(new ResSpeaking(new ResExerciseQuestion(q), subOpt.orElse(null), aiResponse != null ? aiResponse.getResponseJson() : null, objectMapper));
            }

            int taskPercentage = totalPossible > 0 ? (int) Math.round((totalEarned * 100.0) / totalPossible) : 0;
            SchoolLimitAttempts schoolAttemptLimit = schoolAttemptService.getSchoolAttemptLimit(student.getSchool().getUuid());

            if (schoolAttemptLimit.isSetAttempt()) {
                ResAttemptStatus status = schoolAttemptService.getStatus(task.getUuid(),student);
                schoolAttemptLimit.setAttempts(status.getExtraAttempts() + status.getSchoolLimit() - status.getAttemptsUsed());
            }
            ResUnitTaskInfo dto = new ResUnitTaskInfo(task, task.getUuid(), task.getTopic(), task.getTitle(), task.getSortOrder(), (long) questions.size(), taskPercentage, LessonSectionType.SPEAKING, null, speakingData, schoolAttemptLimit);
            dto.setAnsweredAll(!questions.isEmpty() && submissionsCount >= questions.size());
            return dto;
        }).collect(Collectors.toList());
    }

    private List<ResUnitTaskInfo> getVocabTasks(Student student, UUID unitId) {
        System.out.println("getVocabTasks + "+student.getUuid()+"+unitId");
        List<VocabularySet> sets = vocabSetRepo.findAllByUnit_UuidAndStatusOrderBySortOrderAsc(unitId, VocabularySetStatus.ACTIVE);

        if (!userService.getCurrentUser().getRole().equals(UserRole.SYS_ADMIN)) {
            sets = sets.stream().filter(s -> s.getStatus() == VocabularySetStatus.ACTIVE).toList();
        }

        return sets.stream().map(set -> {
            List<VocabularyQuestion> qs = vocabularyQuestionRepository.findAllByVocabularySet_Uuid(set.getUuid());
            long totalPossible = qs.stream().mapToLong(q -> q.getScoreReward() != null ? q.getScoreReward() : 0L).sum();
            System.out.println("Total possible score for set " + set.getUuid() + ": " + totalPossible);
            List<VocabularyAnswer> userAnswers = vocabAnswerRepo.findAllByStudentAndVocabularyQuestionInAndVocabularySet(student, qs, set);
            System.out.println("User answers count for set " + set.getUuid() + ": " + userAnswers.size());
            long totalEarned = userAnswers.stream()
                    .filter(a -> Boolean.TRUE.equals(a.getIsCorrect()))
                    .mapToLong(a -> a.getScore() != null ? a.getScore() : 0L).sum();
            System.out.println("Total earned score for set " + set.getUuid() + ": " + totalEarned);

            int percentage = totalPossible > 0 ? (int) Math.round((totalEarned * 100.0) / totalPossible) : 0;
            System.out.println("Percentage for set " + set.getUuid() + ": " + percentage);
            SchoolLimitAttempts schoolAttemptLimit = schoolAttemptService.getSchoolAttemptLimit(student.getSchool().getUuid());
            System.out.println("School attempt limit for set " + set.getUuid() + ": " + schoolAttemptLimit.getAttempts());
            List<ExerciseTask> allByUnitUuid = taskRepo.findAllByUnit_UuidAndStatus(set.getUnit().getUuid(),ExerciseTaskStatus.ACTIVE);
            System.out.println("All exercise tasks for unit " + set.getUnit().getUuid() + ": " + allByUnitUuid.size());
            UUID taskUuid = null;
            if (!allByUnitUuid.isEmpty()) {
                taskUuid = allByUnitUuid.get(0).getUuid();
            }
            if (schoolAttemptLimit.isSetAttempt()) {
                System.out.println("Checking attempt status for task: " + taskUuid);
                ResAttemptStatus status = schoolAttemptService.getStatus(taskUuid,student);
                schoolAttemptLimit.setAttempts(status.getExtraAttempts() + status.getSchoolLimit() - status.getAttemptsUsed());
            }

            ResUnitTaskInfo dto = new ResUnitTaskInfo(set.getUuid(), set.getTitle(), set.getTitle(),
                    set.getSortOrder(), (long) qs.size(), percentage, LessonSectionType.VOCABULARY, schoolAttemptLimit);
            dto.setAnsweredAll(!qs.isEmpty() && userAnswers.size() >= qs.size());
            dto.setManualGrade(buildManualGradeInfo(userAnswers, VocabularyAnswer::getIsManually, VocabularyAnswer::getUpdatedBy, percentage));
            System.out.println("Returning DTO for set " + set.getUuid() + ": " + dto.getTitle());
            return dto;
        }).collect(Collectors.toList());
    }


    private List<ResUnitTaskInfo> getWritingTasks(Student student, UUID unitId) {
        List<ExerciseTask> tasks = taskRepo.findAllByUnit_UuidAndSectionTypeOrderBySortOrderAsc(unitId, LessonSectionType.WRITING);
        if (!userService.getCurrentUser().getRole().equals(UserRole.SYS_ADMIN)) {
            tasks = tasks.stream().filter(t -> t.getStatus() == ExerciseTaskStatus.ACTIVE).toList();
        }
        return tasks.stream().map(task -> {
            List<ExerciseQuestion> questions = exerciseQuestionRepository.findAllByExerciseTask_UuidAndType(task.getUuid(), ExerciseType.WRITING);
            int submissionsCount = 0;
            List<ResWriting> writingData = new ArrayList<>();
            for (ExerciseQuestion q : questions) {
                Optional<WritingSubmission> subOpt = writingSubmissionRepository.findByStudentUuidAndExerciseQuestionUuid(student.getUuid(), q.getUuid());
                if (subOpt.isPresent()) submissionsCount++;
                writingData.add(new ResWriting(new ResExerciseQuestion(q), subOpt.map(WritingSubmission::getEssay).orElse(""), subOpt.map(WritingSubmission::getFeedback).orElse(""), subOpt.orElse(null)));
            }
            Double avgScore = writingSubmissionRepository.getAverageWritingScoreByStudentAndTask(student.getUuid(), task.getUuid());
            SchoolLimitAttempts schoolAttemptLimit = schoolAttemptService.getSchoolAttemptLimit(student.getSchool().getUuid());

            if (schoolAttemptLimit.isSetAttempt()) {
                ResAttemptStatus status = schoolAttemptService.getStatus(task.getUuid(),student);
                schoolAttemptLimit.setAttempts(status.getExtraAttempts() + status.getSchoolLimit() - status.getAttemptsUsed());
            }

            ResUnitTaskInfo dto = new ResUnitTaskInfo(
                    task, task.getUuid(), task.getTopic(),
                    task.getTitle(), task.getSortOrder(),
                    task.getQuestionCount(), avgScore != null ? avgScore.intValue() : 0,
                    LessonSectionType.WRITING, writingData, schoolAttemptLimit);
            dto.setAnsweredAll(!questions.isEmpty() && submissionsCount >= questions.size());
            return dto;
        }).collect(Collectors.toList());
    }

    private List<ResUnitTaskInfo> getStandardExerciseTasks(Student student, UUID unitId, LessonSectionType type) {
        System.out.println("getStandardExerciseTasks + "+student.getUuid()+"+unitId+"+type);
        List<ExerciseTask> tasks = taskRepo.findAllByUnit_UuidAndSectionTypeOrderBySortOrderAsc(unitId, type);

        if (!userService.getCurrentUser().getRole().equals(UserRole.SYS_ADMIN)) {
            tasks = tasks.stream().filter(t -> t.getStatus() == ExerciseTaskStatus.ACTIVE).toList();
        }

        return tasks.stream().map(task -> {
            List<ExerciseQuestion> questions = questionRepo.findAllByExerciseTask_Uuid(task.getUuid());
            List<ExerciseAnswers> activeAnswers = answerRepo.findAllByStudentAndExerciseQuestionIn(student, questions);

            long totalQuestionsCount = questions.size();
            UUID schoolUuid = student.getSchool().getUuid();

            long totalPossibleScore = 0;
            for (ExerciseQuestion q : questions) {
                totalPossibleScore += rewardCalculationService.getDynamicExerciseReward(q, schoolUuid);
            }

            long totalEarned = activeAnswers.stream()
                    .mapToLong(a -> a.getScore() != null ? a.getScore() : 0L)
                    .sum();

            int percentage = totalPossibleScore > 0 ? (int) Math.round((totalEarned * 100.0) / totalPossibleScore) : 0;
            if (percentage > 100) percentage = 100;

            SchoolLimitAttempts schoolAttemptLimit = schoolAttemptService.getSchoolAttemptLimit(student.getSchool().getUuid());
            if (schoolAttemptLimit.isSetAttempt()) {
                ResAttemptStatus status = schoolAttemptService.getStatus(task.getUuid(),student);
                schoolAttemptLimit.setAttempts(status.getExtraAttempts() + status.getSchoolLimit() - status.getAttemptsUsed());
            }

            ResUnitTaskInfo dto = new ResUnitTaskInfo(task, task.getUuid(), task.getTopic(), task.getTitle(), task.getSortOrder(), totalQuestionsCount, Math.min(percentage, 100), type, schoolAttemptLimit);
            dto.setAnsweredAll(totalQuestionsCount > 0 && activeAnswers.size() >= totalQuestionsCount);
            dto.setManualGrade(buildManualGradeInfo(activeAnswers, ExerciseAnswers::getIsManually, ExerciseAnswers::getUpdatedBy, Math.min(percentage, 100)));
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ResGradingResult grade(UUID questionUuid, ReqExerciseSubmit request) {
        User currentUser = userService.getCurrentUser();
        ExerciseQuestion question = questionRepo.findByUuidAndExerciseTaskUuid(questionUuid, request.getTaskUuid())
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_QUESTION_NOT_FOUND.getKey()));
        ExerciseTask exerciseTask = taskRepo.findByUuid(request.getTaskUuid()).orElse(null);

        if (exerciseTask != null && exerciseTask.getUnit() != null) {
            groupScheduleRepository.findByGroupUuidAndLessonContainingUnit(
                    null, exerciseTask.getUnit().getUuid(), GroupScheduleStatus.ACTIVE
            ).ifPresent(schedule -> {
                if (schedule.getLesson().getCourse().getStatus() == CourseStatus.FINISHED) {
                    throw new ValidationException(MessageKey.COURSE_FINISHED.getKey());
                }
            });
        }

        Student student = null;
        if (UserRole.STUDENT.equals(currentUser.getRole())) {
            student = studentRepo.findByUser(currentUser)
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));
        }

        if (exerciseTask != null && UnitType.EXAM.equals(exerciseTask.getUnit().getUnitType()) && student != null) {
            examService.validateAccess(student, exerciseTask.getUnit(), exerciseTask.getSectionType());

            Student finalStudent = student;
            SchoolExamSettings settings = schoolExamSettingsRepo.findBySchool_Uuid(student.getSchool().getUuid())
                    .orElseGet(() -> {
                        SchoolExamSettings defaults = new SchoolExamSettings();
                        defaults.setSchool(finalStudent.getSchool());
                        defaults.setAttemptLimit(1);
                        return defaults;
                    });

            long attemptCount = answerRepo.countByStudentAndExerciseQuestionAndExerciseTask(student, question, exerciseTask);
            if (attemptCount >= settings.getAttemptLimit()) {
                throw new ValidationException(MessageKey.EXERCISE_MAX_RETRIES.getKey());
            }
        }

        if (question.getType().equals(ExerciseType.SPEAKING)) {
            if (question.getContent() instanceof PronunciationContent) {
                SpeakingSubmission sub = speakingRepo.findByStudent_User_UuidAndExerciseQuestion_Uuid(currentUser.getUuid(), questionUuid)
                        .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_SUBMISSION_NOT_FOUND.getKey()));

                sub.setStatus(ExerciseSubmissionStatus.GRADED);
                speakingRepo.save(sub);

                int bestPercentage = (sub.getScores() != null) ? sub.getScores().getOverallScore() : 0;
                int calculatedReward = rewardCalculationService.getDynamicExerciseReward(question, student.getSchool().getUuid());
                int rawScoreToAward = (int) ((bestPercentage / 100.0) * calculatedReward);

                saveGenericAnswer(student, question, rawScoreToAward, "Pronunciation Submission");
                return new ResGradingResult(true, calculatedReward, "Pronunciation submitted!", null, null, null, bestPercentage);
            }
        }

        ResGradingResult gradeResult = gradingService.grade(question, request);

        if (student == null) return gradeResult;

        if (question.getType().equals(ExerciseType.WRITING)) {
            return gradingService.gradeWriting(question, request, student);
        }

        ExerciseAnswers existingAns = answerRepo.findByStudentAndExerciseQuestionAndExerciseTask(student, question, exerciseTask)
                .orElse(new ExerciseAnswers());

        SchoolAcademicConfig config = schoolAcademicConfigRepo.findBySchool_Uuid(student.getSchool().getUuid())
                .orElse(new SchoolAcademicConfig());

        if (config.isPenaltyEnabled() && existingAns.getId() != null && !Boolean.TRUE.equals(existingAns.getEverCorrect())) {
            if (existingAns.getAttemptCount() >= config.getMaxRetries()) {
                throw new ValidationException(MessageKey.EXERCISE_MAX_RETRIES.getKey());
            }
        }

        if (existingAns.getId() == null) {
            existingAns.setStudent(student);
            existingAns.setExerciseQuestion(question);
            existingAns.setExerciseTask(exerciseTask);
            existingAns.setAttemptCount(0);
        }

        existingAns.setAttemptCount(existingAns.getAttemptCount() + 1);

        try {
            existingAns.setAnswerContent(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            existingAns.setAnswerContent("{}");
        }

        boolean isNowCorrect = gradeResult.correct();
        int calculatedReward = rewardCalculationService.getDynamicExerciseReward(question, student.getSchool().getUuid());
        int weightedScore = (int) ((gradeResult.scorePercentage() / 100.0) * calculatedReward);

        existingAns.setIsCorrect(isNowCorrect);
        existingAns.setScore(weightedScore);
        existingAns.setIsManually(false);
        existingAns.setUpdatedBy(null);

        Integer coinsEarned = 0;
        if (!isNowCorrect) {
            if (config.isPenaltyEnabled() && !Boolean.TRUE.equals(existingAns.getEverCorrect())) {
                student.setCoins(Math.max(0, student.getCoins() - config.getPenaltyPerAttempt()));
                studentRepo.save(student);
            }
        } else if (!Boolean.TRUE.equals(existingAns.getEverCorrect())) {
            existingAns.setEverCorrect(true);
            coinsEarned = calculatedReward;
            awardCoins(student, coinsEarned, weightedScore, "Exercise: " + exerciseTask.getTitle());
        }

        answerRepo.save(existingAns);

        if (exerciseTask != null && exerciseTask.getUnit() != null) {
            progressService.updateUnitProgress(student, exerciseTask.getUnit().getUuid());
        }

        return new ResGradingResult(
                isNowCorrect, coinsEarned,
                isNowCorrect ? "Correct!" : "Incorrect, try again.",
                gradeResult.orderingFeedback(), gradeResult.gapFeedback(),
                gradeResult.matchingFeedback(), gradeResult.scorePercentage()
        );
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

    @Override
    @Transactional
    public ResponseMessage manualGradeUnit(UUID studentUuid, UUID unitUuid, ReqManualGrade request) {
        if (request.getScore() == null || request.getScore() < 0 || request.getScore() > 100) {
            throw new ValidationException(MessageKey.SCORE_OUT_OF_RANGE.getKey());
        }

        Student student = studentRepo.findByUuid(studentUuid)
                .or(() -> studentRepo.findByUserUuid(studentUuid))
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

        authToViewEntity.authorizeActionUponStudent(student);

        Unit unit = unitRepository.findByUuid(unitUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.UNIT_NOT_FOUND.getKey()));

        User currentUser = userService.getCurrentUser();
        UUID schoolUuid = student.getSchool().getUuid();
        int scorePercent = request.getScore();

        SchoolAcademicConfig config = schoolAcademicConfigRepo.findBySchool_Uuid(schoolUuid)
                .orElse(new SchoolAcademicConfig());
        boolean passed = scorePercent >= config.getMinScoreToPass();

        String comment = (request.getComment() != null && !request.getComment().isBlank())
                ? request.getComment() : "Manual Grade";
        String answerContent = "\"" + comment.replace("\"", "\\\"") + "\"";
        String graderName = (currentUser.getFirstName() + " " + currentUser.getLastName()).trim();

        // The dashboard percentage (marks/course) is computed live as earned/possible over the unit's
        // ACTIVE tasks and ACTIVE vocabulary sets (see ProgressService.calculateTotalPossible and the
        // earned-sum queries). Points are distributed over ALL of those questions in ONE pass: rounding
        // each task/set separately lets the per-group errors accumulate, so the unit-level percentage
        // drifted off the entered score (e.g. entering 70 showed 69).
        List<ExerciseTask> tasks = taskRepo.findAllByUnit_UuidAndStatus(unitUuid, ExerciseTaskStatus.ACTIVE);
        List<VocabularySet> vocabSets = vocabSetRepo.findAllByUnit_Uuid(unitUuid).stream()
                .filter(s -> s.getStatus() == VocabularySetStatus.ACTIVE)
                .toList();

        Map<ExerciseTask, List<ExerciseQuestion>> questionsByTask = new LinkedHashMap<>();
        for (ExerciseTask task : tasks) {
            List<ExerciseQuestion> questions = questionRepo.findAllByExerciseTask_Uuid(task.getUuid());
            if (!questions.isEmpty()) {
                questionsByTask.put(task, questions);
            }
        }
        int vocabRewardPerWord = rewardCalculationService.getDynamicVocabReward(schoolUuid, 2);
        Map<VocabularySet, List<VocabularyQuestion>> wordsBySet = new LinkedHashMap<>();
        for (VocabularySet set : vocabSets) {
            List<VocabularyQuestion> words = vocabularyQuestionRepository.findAllByVocabularySet_Uuid(set.getUuid());
            if (!words.isEmpty()) {
                wordsBySet.put(set, words);
            }
        }

        int totalQuestions = questionsByTask.values().stream().mapToInt(List::size).sum()
                + wordsBySet.values().stream().mapToInt(List::size).sum();
        if (totalQuestions == 0) {
            throw new ValidationException(MessageKey.EXERCISE_QUESTION_NOT_FOUND.getKey());
        }

        int[] rewards = new int[totalQuestions];
        int idx = 0;
        for (List<ExerciseQuestion> questions : questionsByTask.values()) {
            for (ExerciseQuestion question : questions) {
                rewards[idx++] = rewardCalculationService.getDynamicExerciseReward(question, schoolUuid);
            }
        }
        for (List<VocabularyQuestion> words : wordsBySet.values()) {
            for (int i = 0; i < words.size(); i++) {
                rewards[idx++] = vocabRewardPerWord;
            }
        }
        int[] awarded = distributeScore(rewards, scorePercent);

        int defaultCoins = 0;
        int weightedScoreAwarded = 0;
        boolean anyNewlyPassed = false;
        idx = 0;

        for (Map.Entry<ExerciseTask, List<ExerciseQuestion>> entry : questionsByTask.entrySet()) {
            ExerciseTask task = entry.getKey();
            for (ExerciseQuestion question : entry.getValue()) {
                ExerciseAnswers ans = answerRepo.findByStudentAndExerciseQuestionAndExerciseTask(student, question, task)
                        .orElseGet(ExerciseAnswers::new);

                if (ans.getId() == null) {
                    ans.setStudent(student);
                    ans.setExerciseQuestion(question);
                    ans.setExerciseTask(task);
                    ans.setAttemptCount(0);
                }

                ans.setIsCorrect(passed);
                ans.setScore(awarded[idx]);
                ans.setAnswerContent(answerContent);
                ans.setIsManually(true);
                ans.setUpdatedBy(graderName);

                if (passed && !Boolean.TRUE.equals(ans.getEverCorrect())) {
                    ans.setEverCorrect(true);
                    anyNewlyPassed = true;
                    defaultCoins += rewards[idx];
                    weightedScoreAwarded += awarded[idx];
                }

                answerRepo.save(ans);
                idx++;
            }
        }

        for (Map.Entry<VocabularySet, List<VocabularyQuestion>> entry : wordsBySet.entrySet()) {
            VocabularySet set = entry.getKey();
            for (VocabularyQuestion word : entry.getValue()) {
                VocabularyAnswer va = vocabAnswerRepo.findByStudentAndVocabularyQuestion(student, word)
                        .orElseGet(VocabularyAnswer::new);

                boolean wasCorrect = Boolean.TRUE.equals(va.getIsCorrect());
                if (va.getId() == null) {
                    va.setStudent(student);
                    va.setVocabularyQuestion(word);
                    va.setVocabularySet(set);
                    va.setAttemptCount(0);
                }

                va.setIsCorrect(passed);
                va.setScore(awarded[idx]);
                va.setAnswerContent(answerContent);
                va.setIsManually(true);
                va.setUpdatedBy(graderName);

                if (passed && !wasCorrect) {
                    anyNewlyPassed = true;
                    defaultCoins += rewards[idx];
                    weightedScoreAwarded += awarded[idx];
                }

                vocabAnswerRepo.save(va);
                idx++;
            }
        }

        if (anyNewlyPassed) {
            int coinsToAward = (request.getCoins() != null) ? request.getCoins() : defaultCoins;

            student.setCoins(student.getCoins() + coinsToAward);
            student.setLifeTimeCoinBalance(student.getLifeTimeCoinBalance() + (long) coinsToAward);
            student.setScore(student.getScore() + weightedScoreAwarded);
            studentRepo.save(student);

            CoinTransaction tx = new CoinTransaction();
            tx.setStudent(student);
            tx.setAmount(coinsToAward);
            tx.setType(TransactionType.ADMINISTRATION_AWARD);
            tx.setReason("Manually Graded Unit: " + unit.getTitle());
            tx.setGivenBy(currentUser.getUsername());
            coinTransactionRepo.save(tx);
        }

        progressService.updateUnitProgress(student, unit.getUuid());

        return new ResponseMessage(passed ? "Unit graded and passed!" : "Grade saved. Student needs to retry.");
    }

    /**
     * A task/set is considered "manually graded" only when every existing answer in it carries the
     * manual-grade marker - a partially auto-answered task must not be reported as manually graded.
     */
    private <T> ManualGradeInfo buildManualGradeInfo(List<T> answers, java.util.function.Function<T, Boolean> isManuallyFn,
                                                      java.util.function.Function<T, String> updatedByFn, int score) {
        boolean isManually = !answers.isEmpty() && answers.stream().allMatch(a -> Boolean.TRUE.equals(isManuallyFn.apply(a)));
        if (!isManually) {
            return new ManualGradeInfo(false, null, null);
        }
        String updatedBy = answers.stream().map(updatedByFn).filter(Objects::nonNull).findFirst().orElse(null);
        return new ManualGradeInfo(true, updatedBy, score);
    }

    /**
     * Splits {@code percent}% of each reward into integer points whose sum equals
     * {@code round(percent/100 * sum(rewards))}. This makes the group's earned/possible percentage equal
     * {@code percent} exactly (within integer granularity), instead of the per-question truncation that
     * systematically loses points. Each entry gets its floored share first; the leftover points then go
     * to the entries with the largest fractional remainders.
     */
    private int[] distributeScore(int[] rewards, int percent) {
        int n = rewards.length;
        int[] awarded = new int[n];
        if (n == 0) return awarded;

        long totalPossible = 0;
        for (int r : rewards) totalPossible += r;
        long target = Math.round(totalPossible * percent / 100.0);
        // Prefer a total that rounds back to exactly `percent` when displayed as earned/possible:
        // with small denominators the plain rounded target can land on a neighboring percent.
        for (long cand : new long[]{target, target - 1, target + 1}) {
            if (cand >= 0 && cand <= totalPossible
                    && Math.round(cand * 100.0 / totalPossible) == percent) {
                target = cand;
                break;
            }
        }

        double[] frac = new double[n];
        long allocated = 0;
        for (int i = 0; i < n; i++) {
            double exact = rewards[i] * percent / 100.0;
            awarded[i] = (int) Math.floor(exact);
            frac[i] = exact - awarded[i];
            allocated += awarded[i];
        }

        long remainder = target - allocated; // points still to hand out (can slightly exceed n after the target adjustment)
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> {
            int c = Double.compare(frac[b], frac[a]);
            return c != 0 ? c : Integer.compare(rewards[b], rewards[a]);
        });
        for (long k = 0; k < remainder; k++) {
            awarded[order[(int) (k % n)]] += 1;
        }
        return awarded;
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

        int possibleReward = q.getScoreReward() != null ? q.getScoreReward() : 0;
        int weightedScore = (int) ((score / 100.0) * possibleReward);
        ans.setScore(weightedScore);
        ans.setIsManually(false);
        ans.setUpdatedBy(null);

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
    @Transactional(readOnly = true)
    public ResQuestionsTasks getQuestionsForTask(UUID taskUuid) {
        User user = userService.getCurrentUser();
        Student student = null;
        if (UserRole.STUDENT.equals(user.getRole())) {
            student = studentRepo.findByUser(user)
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));
        }

        ExerciseTask task = taskRepo.findByUuid(taskUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_TASK_NOT_FOUND.getKey()));

        boolean isExam = UnitType.EXAM.equals(task.getUnit().getUnitType());
        if (isExam && student != null) {
            examService.validateAccess(student, task.getUnit(), task.getSectionType());
        }

        List<ExerciseQuestion> questions = questionRepo.findAllByExerciseTask_Uuid(taskUuid);
        List<ExerciseAnswers> answers = student != null ?
                answerRepo.findAllByStudentAndExerciseQuestionIn(student, questions) : new ArrayList<>();

        Map<UUID, ExerciseAnswers> answeredMap = answers.stream()
                .collect(Collectors.toMap(a -> a.getExerciseQuestion().getUuid(), a -> a, (a1, a2) -> a1));

        List<ResStudentQuestion> studentQuestions = new ArrayList<>();
        for (ExerciseQuestion q : questions) {
            ExerciseAnswers attempt = answeredMap.get(q.getUuid());

            if (isExam && attempt != null) continue;
            if (!isExam && !q.getType().equals(ExerciseType.WRITING) && !q.getType().equals(ExerciseType.SPEAKING)) {
                if (attempt != null && (Boolean.TRUE.equals(attempt.getEverCorrect()) || Boolean.TRUE.equals(attempt.getIsCorrect()))) {
                    continue;
                }
            }

            ResStudentQuestion sq = new ResStudentQuestion();
            sq.setQuestionId(q.getUuid());
            sq.setType(q.getType());
            sq.setExerciseId(taskUuid);
            sq.setInstruction(q.getInstruction());
            sq.setCoinReward(q.getCoinReward());

            if (q.getType() == ExerciseType.TRACING && q.getContent() instanceof CircleTracingContent tracingContent) {
                String wordPattern = tracingContent.getTargetWord();

                Map<String, String> pMap = new HashMap<>();
                if (q.getAnswerKey() instanceof TracingKey tk) {
                    pMap = tk.getPlaceholderMap();
                } else if (q.getAnswerKey() != null) {
                    try {
                        Map<String, Object> rawKey = objectMapper.convertValue(q.getAnswerKey(), Map.class);
                        if (rawKey.containsKey("placeholderMap")) {
                            pMap = (Map<String, String>) rawKey.get("placeholderMap");
                        }
                    } catch (Exception ignored) {
                    }
                }

                List<CircleTracingContent.LetterData> letterDataList = new ArrayList<>();
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\{\\{(\\d+)\\}\\})|.");
                java.util.regex.Matcher matcher = pattern.matcher(wordPattern);

                Set<String> charsToFetch = new HashSet<>();
                List<Object[]> sequence = new ArrayList<>();

                while (matcher.find()) {
                    if (matcher.group(1) != null) {
                        String id = matcher.group(2);
                        String character = pMap != null ? pMap.getOrDefault(id, "") : "";
                        sequence.add(new Object[]{character, id});
                        if (!character.isEmpty()) charsToFetch.add(character);
                    } else {
                        String match = matcher.group();
                        sequence.add(new Object[]{match, null});
                        charsToFetch.add(match);
                    }
                }

                Map<String, List<String>> svgMap = charsToFetch.isEmpty() ? new HashMap<>() :
                        characterSvgRepository.findAllByCharValueIn(charsToFetch)
                                .stream().collect(Collectors.toMap(CharacterSvg::getCharValue, CharacterSvg::getSvgPaths, (a, b) -> a));

                for (Object[] part : sequence) {
                    String character = (String) part[0];
                    String pId = (String) part[1];
                    letterDataList.add(new CircleTracingContent.LetterData(
                            character,
                            svgMap.getOrDefault(character, new ArrayList<>()),
                            pId != null,
                            pId
                    ));
                }
                tracingContent.setLetters(letterDataList);
            }

            sq.setContent(q.getContent());

            if (q.getType().equals(ExerciseType.WRITING) && student != null) {
                writingSubmissionRepository.findByStudentUuidAndExerciseQuestionUuid(student.getUuid(), q.getUuid())
                        .ifPresent(sub -> {
                            sq.setSubmissionStatus(sub.getStatus());
                            sq.setStudentEssay(sub.getEssay());
                            sq.setTeacherFeedback(sub.getFeedback());
                        });
                if (sq.getSubmissionStatus() == null) sq.setSubmissionStatus(ExerciseSubmissionStatus.NOT_STARTED);
            } else if (q.getType().equals(ExerciseType.SPEAKING) && student != null) {
                speakingRepo.findByStudent_User_UuidAndExerciseQuestion_Uuid(student.getUser().getUuid(), q.getUuid())
                        .ifPresent(ss -> {
                            sq.setSubmissionStatus(ss.getStatus());
                            sq.setTranscript(ss.getTranscript());
                            if (ss.getAudioAttachment() != null) {
                                sq.setStudentAudioUrl(CoreUtils.getBaseFileUrl() + ss.getAudioAttachment().getName());
                            }
                            if (ss.getStatus().equals(ExerciseSubmissionStatus.GRADED)) {
                                sq.setSpeakingScores(ss.getScores());
                                sq.setFeedbackBullets(ss.getFeedbackBullets());
                            }
                        });
                if (sq.getSubmissionStatus() == null) sq.setSubmissionStatus(ExerciseSubmissionStatus.NOT_STARTED);
            }

            if (q.getType().equals(ExerciseType.GAP_FILL) && attempt != null) {
                sq.setPreFilledAnswers(extractCorrectGaps(q, attempt.getAnswerContent()));
            }
            studentQuestions.add(sq);
        }

        ResQuestionsTasks response = new ResQuestionsTasks(task.getTitle(), (long) studentQuestions.size(), String.valueOf(taskUuid), studentQuestions);

        if (student != null) {
            int limit = schoolAcademicConfigRepo.findBySchool_Uuid(student.getSchool().getUuid())
                    .map(c -> (c.getHomeworkAttemptLimit() != null && c.getHomeworkAttemptLimit() > 0) ? c.getHomeworkAttemptLimit() : 5)
                    .orElse(5);
            var taskAttempt = taskAttemptRepository.findByStudent_UuidAndTask_Uuid(student.getUuid(), taskUuid).orElse(null);
            int used = (taskAttempt != null) ? taskAttempt.getAttemptsUsed() : 0;
            int extra = (taskAttempt != null) ? taskAttempt.getExtraAttempts() : 0;
            response.setAttemptLimit(limit);
            response.setExtraAttempts(extra);
            response.setAttemptsUsed(used);
            response.setRemainingAttempts(Math.max(0, (limit + extra) - used));
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ResTaskResult getTaskResult(UUID taskUuid, Boolean flag) {
        flag = (flag != null) ? flag : false;
        User currentUser = userService.getCurrentUser();
        Student student = studentRepo.findByUser(currentUser)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

        return getTaskResultInternal(student, taskUuid, flag);
    }

    @Override
    @Transactional(readOnly = true)
    public ResTaskResult getTaskResultForStudent(UUID userUuid, UUID taskUuid, Boolean flag) {
        // The caller may pass either the user UUID or the student UUID - accept both.
        Student student = studentRepo.findByUserUuid(userUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

        authToViewEntity.authorizeActionUponStudent(student);

        ExerciseTask task = taskRepo.findByUuid(taskUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_TASK_NOT_FOUND.getKey()));

        return getTaskResultInternal(student, taskUuid, flag);
    }

    private boolean checkEditPermission(SchoolBook book) {
        User user = userService.getCurrentUser();
        if (user.getRole() == UserRole.SYS_ADMIN) return true;
        if (book.isGlobal()) return false;

        List<UUID> authorizedUuids = userScopeService.getAuthorizedSchoolUuids();
        if (book.getSchool() != null && authorizedUuids.contains(book.getSchool().getUuid())) return true;

        if (user.getRole() == UserRole.SCHOOL_DIRECTOR && book.getOrganization() != null) {
            return schoolDirectorRepo.findByUser(user)
                    .map(d -> d.getOrganization().equals(book.getOrganization()))
                    .orElse(false);
        }
        return false;
    }

    private ResTaskResult getTaskResultInternal(Student student, UUID taskUuid, Boolean flag) {
        flag = (flag != null) ? flag : false;
        ExerciseTask task = taskRepo.findByUuid(taskUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_TASK_NOT_FOUND.getKey()));

        List<ExerciseQuestion> questions = questionRepo.findAllByExerciseTask_Uuid(taskUuid);
        List<ExerciseAnswers> answers = answerRepo.findAllByStudentAndExerciseQuestionIn(student, questions);
        Map<UUID, ExerciseAnswers> answerMap = answers.stream()
                .collect(Collectors.toMap(a -> a.getExerciseQuestion().getUuid(), a -> a));

        int correctCount = 0, totalScore = 0, totalCoinsEarned = 0;
        long totalPossibleScore = 0;
        for (ExerciseQuestion q : questions) {
            totalPossibleScore += rewardCalculationService.getDynamicExerciseReward(q, student.getSchool().getUuid());
        }

        List<ResTaskResult.QuestionResultSummary> summaries = new ArrayList<>();
        List<UUID> mistakesToAnalyze = new ArrayList<>();
        boolean aiEnabled = schoolSubscriptionRepo.findBySchool_Uuid(student.getSchool().getUuid())
                .map(SchoolSubscription::isAiExerciseEnabled).orElse(false);

        for (ExerciseQuestion q : questions) {
            ExerciseAnswers ans = answerMap.get(q.getUuid());
            boolean isCorrect = ans != null && Boolean.TRUE.equals(ans.getIsCorrect());
            int dynamicReward = rewardCalculationService.getDynamicExerciseReward(q, student.getSchool().getUuid());

            Object studentAnswer = null;
            Object correctAnswer = null;

            try {
                if (q.getType() == ExerciseType.MATCHING) {
                    uz.tune.mentourBiz.rest.payload.questionFormats.types.nodes.MatchingContent mc = (uz.tune.mentourBiz.rest.payload.questionFormats.types.nodes.MatchingContent) q.getContent();
                    Map<String, String> lookup = new HashMap<>();
                    mc.getLeftItems().forEach(lookup::putAll);
                    mc.getRightItems().forEach(lookup::putAll);

                    MatchingKey key = (MatchingKey) q.getAnswerKey();
                    Map<String, String> cMap = new HashMap<>();
                    key.getPairs().forEach((l, r) -> cMap.put(lookup.getOrDefault(l, l), lookup.getOrDefault(r, r)));
                    correctAnswer = cMap;

                    if (ans != null) {
                        ReqExerciseSubmit sub = objectMapper.readValue(ans.getAnswerContent(), ReqExerciseSubmit.class);
                        Map<String, String> sMap = new HashMap<>();
                        if (sub.getMatchingPairs() != null)
                            sub.getMatchingPairs().forEach((l, r) -> sMap.put(lookup.getOrDefault(l, l), lookup.getOrDefault(r, r)));
                        studentAnswer = sMap;
                    }
                } else if (q.getType() == ExerciseType.SELECTION || q.getType() == ExerciseType.MULTI_SELECT) {
                    uz.tune.mentourBiz.rest.payload.questionFormats.types.nodes.SelectionContent sc = (uz.tune.mentourBiz.rest.payload.questionFormats.types.nodes.SelectionContent) q.getContent();
                    Map<String, String> lookup = new HashMap<>();
                    sc.getOptions().forEach(lookup::putAll);

                    if (q.getType() == ExerciseType.SELECTION) {
                        correctAnswer = lookup.getOrDefault(((SelectionKey) q.getAnswerKey()).getCorrectOptionId(), "");
                        if (ans != null) {
                            ReqExerciseSubmit sub = objectMapper.readValue(ans.getAnswerContent(), ReqExerciseSubmit.class);
                            studentAnswer = lookup.getOrDefault(sub.getSelectedOptionId(), sub.getSelectedOptionId());
                        }
                    } else {
                        List<String> cList = ((MultiSelectKey) q.getAnswerKey()).getCorrectOptionIds();
                        correctAnswer = cList.stream().map(id -> lookup.getOrDefault(id, id)).toList();
                        if (ans != null) {
                            ReqExerciseSubmit sub = objectMapper.readValue(ans.getAnswerContent(), ReqExerciseSubmit.class);
                            studentAnswer = sub.getMultiSelectAnswer().stream().map(id -> lookup.getOrDefault(id, id)).toList();
                        }
                    }
                } else if (q.getType() == ExerciseType.CIRCLE) {
                    uz.tune.mentourBiz.qs.CircleContent cc = (uz.tune.mentourBiz.qs.CircleContent) q.getContent();
                    Map<String, String> lookup = new HashMap<>();
                    cc.getParts().forEach(p -> {
                        if (p.getChars() != null) p.getChars().forEach(c -> lookup.put(c.getId(), c.getValue()));
                    });

                    correctAnswer = ((uz.tune.mentourBiz.qs.CircleKey) q.getAnswerKey()).getCorrectCharIds().stream().map(id -> lookup.getOrDefault(id, id)).toList();
                    if (ans != null) {
                        ReqExerciseSubmit sub = objectMapper.readValue(ans.getAnswerContent(), ReqExerciseSubmit.class);
                        studentAnswer = sub.getMultiSelectAnswer().stream().map(id -> lookup.getOrDefault(id, id)).toList();
                    }
                } else {
                    correctAnswer = q.getAnswerKey();
                    if (ans != null) {
                        String content = ans.getAnswerContent();
                        if (content.startsWith("{")) {
                            ReqExerciseSubmit sub = objectMapper.readValue(content, ReqExerciseSubmit.class);
                            studentAnswer = switch (q.getType()) {
                                case GAP_FILL -> sub.getAnswers();
                                case ORDERING -> sub.getOrderingAnswer();
                                case WRITING -> sub.getWritingText();
                                case TRACING -> sub.getTracingResults();
                                default -> content;
                            };
                        } else {
                            studentAnswer = content.replace("\"", "");
                        }
                    }
                }
            } catch (Exception e) {
                studentAnswer = ans != null ? ans.getAnswerContent() : null;
                correctAnswer = q.getAnswerKey();
            }

            int scoreEarned = (ans != null && ans.getScore() != null) ? ans.getScore() : 0;
            totalScore += scoreEarned;
            if (isCorrect) {
                correctCount++;
                totalCoinsEarned += dynamicReward;
            } else if (Boolean.TRUE.equals(flag) && aiEnabled && ans != null && ans.getErrorExplanation() == null) {
                if (q.getType() != ExerciseType.SPEAKING && q.getType() != ExerciseType.TRACING)
                    mistakesToAnalyze.add(ans.getSubmissionId());
            }

            // Per-item feedback (gap / ordering position / matching pair) is not persisted at submit
            // time, so recompute it here; without it the result screen marks the whole question wrong
            // when even one item is wrong.
            Map<String, Boolean> gapFeedback =
                    q.getType() == ExerciseType.GAP_FILL ? buildGapFeedback(q, ans) : null;
            List<Boolean> orderingFeedback =
                    q.getType() == ExerciseType.ORDERING ? buildOrderingFeedback(q, ans) : null;
            Map<String, Boolean> matchingFeedback =
                    q.getType() == ExerciseType.MATCHING ? buildMatchingFeedback(q, ans) : null;

            summaries.add(new ResTaskResult.QuestionResultSummary(
                    q.getUuid(), q.getType(), isCorrect, isCorrect ? dynamicReward : 0, scoreEarned,
                    studentAnswer, correctAnswer, orderingFeedback, gapFeedback, matchingFeedback, scoreEarned, null,
                    ans != null ? ans.getErrorExplanation() : null
            ));
        }

        if (!mistakesToAnalyze.isEmpty()) {
            System.out.println("Ai request");
            aiExplanationService.processMistakesBatchAsync(mistakesToAnalyze, null, GlobalVar.getLang(), GlobalVar.getUsername(), GlobalVar.getRequestId());
        }

        int percentage = (totalPossibleScore > 0) ? (int) Math.round(((double) totalScore / totalPossibleScore) * 100) : 0;
        ResTaskResult result = new ResTaskResult(taskUuid, task.getTitle(), questions.size(), correctCount, totalCoinsEarned, totalScore, Math.min(percentage, 100), summaries);
        result.setAiExplanationEnabled(aiEnabled);
        return result;
    }

    private Object parseSpecificAnswer(ExerciseType type, String jsonContent) {
        try {
            ReqExerciseSubmit submit = objectMapper.readValue(jsonContent, ReqExerciseSubmit.class);
            return switch (type) {
                case GAP_FILL -> submit.getAnswers();          //  Map<String, String>
                case SELECTION -> submit.getSelectedOptionId(); //  String
                case ORDERING -> submit.getOrderingAnswer();   //  List<String>
                case MATCHING -> submit.getMatchingPairs();    //  Map<String, String>
                case WRITING -> submit.getWritingText();       //  String
                default -> jsonContent;
            };
        } catch (Exception e) {
            return jsonContent;
        }
    }

    /**
     * Rebuilds the per-gap correctness map for a GAP_FILL answer at result-viewing time. The map is
     * computed at submit time but never persisted (only the whole-question isCorrect/score are), so
     * the result screen must recompute it — otherwise it has no per-gap data and paints every gap as
     * wrong whenever any single gap is wrong. Keyed by gapId; gaps the student left blank are false.
     */
    private Map<String, Boolean> buildGapFeedback(ExerciseQuestion q, ExerciseAnswers ans) {
        Map<String, Boolean> feedback = new HashMap<>();
        if (ans == null) return feedback;
        try {
            GapFillKey key = (GapFillKey) q.getAnswerKey();
            ReqExerciseSubmit sub = objectMapper.readValue(ans.getAnswerContent(), ReqExerciseSubmit.class);
            Map<String, String> userAnswers = sub.getAnswers() != null ? sub.getAnswers() : Collections.emptyMap();
            key.getAnswers().keySet().forEach(gapId ->
                    feedback.put(gapId, gradingService.isGapCorrect(key, gapId, userAnswers.get(gapId))));
        } catch (Exception ignored) {
        }
        return feedback;
    }

    /**
     * Rebuilds the per-position correctness list for an ORDERING answer at result-viewing time. Like
     * {@link #buildGapFeedback}, this is computed at submit time but never persisted, so the result
     * screen must recompute it or it marks the whole question wrong when any single position is wrong.
     */
    private List<Boolean> buildOrderingFeedback(ExerciseQuestion q, ExerciseAnswers ans) {
        if (ans == null) return null;
        try {
            OrderingKey key = (OrderingKey) q.getAnswerKey();
            ReqExerciseSubmit sub = objectMapper.readValue(ans.getAnswerContent(), ReqExerciseSubmit.class);
            if (sub.getOrderingAnswer() == null) return null;
            return gradingService.getGranularOrderingFeedback(key, sub.getOrderingAnswer());
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Rebuilds the per-pair correctness map for a MATCHING answer at result-viewing time (keyed by the
     * left item id, same as at submit time). Not persisted at submit time, so recompute it here to
     * avoid the "one wrong pair marks all pairs wrong" display bug.
     */
    private Map<String, Boolean> buildMatchingFeedback(ExerciseQuestion q, ExerciseAnswers ans) {
        if (ans == null) return null;
        try {
            MatchingKey key = (MatchingKey) q.getAnswerKey();
            ReqExerciseSubmit sub = objectMapper.readValue(ans.getAnswerContent(), ReqExerciseSubmit.class);
            Map<String, String> userPairs = sub.getMatchingPairs() != null ? sub.getMatchingPairs() : Collections.emptyMap();
            return gradingService.gradeMatching(key, userPairs);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<String, String> extractCorrectGaps(ExerciseQuestion q, String jsonContent) {
        Map<String, String> correctPreFills = new HashMap<>();
        try {
            ReqExerciseSubmit lastSubmit = objectMapper.readValue(jsonContent, ReqExerciseSubmit.class);
            if (lastSubmit.getAnswers() != null) {
                GapFillKey key = (GapFillKey) q.getAnswerKey();
                lastSubmit.getAnswers().forEach((gapId, value) -> {
                    if (gradingService.isGapCorrect(key, gapId, value)) {
                        correctPreFills.put(gapId, value);
                    }
                });
            }
        } catch (Exception ignored) {
        }
        return correctPreFills;
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

