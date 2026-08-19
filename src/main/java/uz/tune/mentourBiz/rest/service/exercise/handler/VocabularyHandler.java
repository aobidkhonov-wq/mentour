package uz.tune.mentourBiz.rest.service.exercise.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.vocabulary.VocabularySet;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.enums.LessonSectionType;
import uz.tune.mentourBiz.rest.enums.VocabularySetStatus;
import uz.tune.mentourBiz.rest.payload.studentRes.res.lesson.ResLessonSection;
import uz.tune.mentourBiz.rest.repository.unit.VocabularyAnswerRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.vocabulary.VocabularyQuestionRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.vocabulary.VocabularySetRepository;
import uz.tune.mentourBiz.rest.service.RewardCalculationService;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VocabularyHandler implements SectionHandler {

    private final VocabularyQuestionRepository vocabQuestionRepo;
    private final VocabularyAnswerRepository vocabAnswerRepo;
    private final VocabularySetRepository vocabularySetRepository;
    private final RewardCalculationService rewardCalculationService;


    @Override
    public ResLessonSection getSectionProgress(Student student, Unit unit) {
        List<VocabularySet> sets = vocabularySetRepository.findAllByUnit_UuidAndStatusOrderBySortOrderAsc(unit.getUuid(), VocabularySetStatus.ACTIVE);
        if (sets.isEmpty()) return null;

        UUID schoolUuid = student.getSchool().getUuid();
        int rewardPerWord = rewardCalculationService.getDynamicVocabReward(schoolUuid, 2);

        long totalPossible = 0;
        for (VocabularySet set : sets) {
            totalPossible += (long) set.getQuestionCount() * rewardPerWord;
        }

        if (totalPossible == 0) return new ResLessonSection(LessonSectionType.VOCABULARY, "VOCABULARY", 0, false);

        Long totalEarned = vocabAnswerRepo.sumTotalEarnedVocabScoreByStudentAndUnit(student.getId(), unit.getUuid());
        int percent = (int) Math.round((double) (totalEarned != null ? totalEarned : 0) / totalPossible * 100);

        return new ResLessonSection(LessonSectionType.VOCABULARY, "VOCABULARY", Math.min(percent, 100), false);
    }
}