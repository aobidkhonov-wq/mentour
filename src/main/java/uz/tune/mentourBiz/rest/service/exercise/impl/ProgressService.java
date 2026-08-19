package uz.tune.mentourBiz.rest.service.exercise.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.rest.domain.SchoolAcademicConfig;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseQuestion;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseTask;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.postExercise.UnitProgress;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularySet;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.enums.*;
import uz.tune.mentourBiz.rest.repository.SchoolAcademicConfigRepo;
import uz.tune.mentourBiz.rest.repository.unit.UnitRepository;
import uz.tune.mentourBiz.rest.repository.unit.VocabularyAnswerRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseAnswersRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseQuestionRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseTaskRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.UnitProgressRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.vocabulary.VocabularyQuestionRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.vocabulary.VocabularySetRepository;
import uz.tune.mentourBiz.rest.service.RewardCalculationService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final UnitProgressRepository unitProgressRepo;
    private final ExerciseAnswersRepository answerRepo;
    private final VocabularyAnswerRepository vocabularyAnswerRepo;
    private final UnitRepository unitRepository;
    private final ExerciseQuestionRepository exerciseQuestionRepository;
    private final VocabularyQuestionRepository vocabularyQuestionRepository;
    private final SchoolAcademicConfigRepo schoolAcademicConfigRepo;
    private final ExerciseTaskRepository exerciseTaskRepository;
    private final RewardCalculationService rewardCalculationService;
    private final VocabularySetRepository vocabularySetRepository;

    @Transactional
    public void updateUnitProgress(Student student, UUID unitUuid) {
        Unit unit = unitRepository.findByUuid(unitUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.UNIT_NOT_FOUND.getKey()));

        if (unit.getStatus().equals(UnitStatus.INACTIVE)) return;

        UnitProgress progress = unitProgressRepo.findByStudentAndUnit(student, unit)
                .orElse(new UnitProgress());

        if (progress.getId() == null) {
            progress.setStudent(student);
            progress.setUnit(unit);
            progress.setStatus(UnitProgressStatus.ATTEMPTED);
        }

        UUID schoolUuid = student.getSchool().getUuid();

        long totalPossible = calculateTotalPossible(unit, schoolUuid);
        long totalEarned = calculateTotalEarned(student, unit.getUuid());

        progress.setProgressPercentage(toPercentage(totalEarned, totalPossible));

        SchoolAcademicConfig config = schoolAcademicConfigRepo.findBySchool_Uuid(schoolUuid)
                .orElse(new SchoolAcademicConfig());

        if (progress.getProgressPercentage() >= config.getMinScoreToPass()) {
            progress.setStatus(UnitProgressStatus.COMPLETED);
        } else if (progress.getStatus() != UnitProgressStatus.COMPLETED) {
            progress.setStatus(UnitProgressStatus.ATTEMPTED);
        }

        unitProgressRepo.save(progress);
    }

    /**
     * Unit bo'yicha jami mumkin bo'lgan ball (faqat ACTIVE tasklar/setlar).
     * Bu — barcha joydagi foiz hisobining yagona maxraji (single source of truth).
     */
    @Transactional(readOnly = true)
    public long calculateTotalPossible(Unit unit, UUID schoolUuid) {
        long totalPossible = 0;

        List<ExerciseTask> allTasks = exerciseTaskRepository.findAllByUnit_UuidAndStatus(unit.getUuid(), ExerciseTaskStatus.ACTIVE);
        for (ExerciseTask t : allTasks) {
            List<ExerciseQuestion> questions = exerciseQuestionRepository.findAllByExerciseTask_Uuid(t.getUuid());
            for (ExerciseQuestion q : questions) {
                totalPossible += rewardCalculationService.getDynamicExerciseReward(q, schoolUuid);
            }
        }

        int vocabRewardPerWord = rewardCalculationService.getDynamicVocabReward(schoolUuid, 2);
        List<VocabularySet> allSets = vocabularySetRepository.findAllByUnit_Uuid(unit.getUuid());
        for (VocabularySet s : allSets) {
            if (s.getStatus() == VocabularySetStatus.ACTIVE) {
                totalPossible += ((long) s.getQuestionCount() * vocabRewardPerWord);
            }
        }
        return totalPossible;
    }

    /** Student shu unit bo'yicha to'plagan jami ball (faqat ACTIVE tasklar/setlar). */
    @Transactional(readOnly = true)
    public long calculateTotalEarned(Student student, UUID unitUuid) {
        Long earnedEx = answerRepo.sumTotalEarnedScoreByStudentAndUnit(student.getId(), unitUuid);
        Long earnedVocab = vocabularyAnswerRepo.sumTotalEarnedVocabScoreByStudentAndUnit(student.getId(), unitUuid);
        return (earnedEx != null ? earnedEx : 0L) + (earnedVocab != null ? earnedVocab : 0L);
    }

    /** Yagona yumaloqlash qoidasi: round, 0..100 oralig'ida. */
    public int toPercentage(long totalEarned, long totalPossible) {
        if (totalPossible <= 0) return 0;
        return Math.min((int) Math.round(((double) totalEarned / totalPossible) * 100), 100);
    }
}