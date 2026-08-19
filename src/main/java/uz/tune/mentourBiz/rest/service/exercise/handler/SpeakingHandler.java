package uz.tune.mentourBiz.rest.service.exercise.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseQuestion;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseTask;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.enums.ExerciseTaskStatus;
import uz.tune.mentourBiz.rest.enums.LessonSectionType;
import uz.tune.mentourBiz.rest.payload.studentRes.res.lesson.ResLessonSection;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseAnswersRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseQuestionRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseTaskRepository;
import uz.tune.mentourBiz.rest.service.RewardCalculationService;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SpeakingHandler implements SectionHandler {

    private final ExerciseQuestionRepository questionRepo;
    private final ExerciseAnswersRepository answerRepo;
    private final ExerciseTaskRepository exerciseTaskRepository;
    private final RewardCalculationService rewardCalculationService;

    @Override
    public ResLessonSection getSectionProgress(Student student, Unit unit) {
        List<ExerciseTask> tasks = exerciseTaskRepository.findAllByUnit_UuidAndSectionTypeAndStatus(unit.getUuid(), LessonSectionType.SPEAKING, ExerciseTaskStatus.ACTIVE);
        if (tasks.isEmpty()) return null;

        long totalPossible = 0;
        for (ExerciseTask task : tasks) {
            List<ExerciseQuestion> questions = questionRepo.findAllByExerciseTask_Uuid(task.getUuid());
            for (ExerciseQuestion q : questions) {
                totalPossible += rewardCalculationService.getDynamicExerciseReward(q, student.getSchool().getUuid());
            }
        }

        if (totalPossible == 0) return new ResLessonSection(LessonSectionType.SPEAKING, "SPEAKING", 0, false);

        Long totalEarned = answerRepo.sumTotalEarnedScoreByStudentAndUnitAndSectionType(
                student.getId(), unit.getUuid(), LessonSectionType.SPEAKING);

        int percent = (int) Math.round((double) (totalEarned != null ? totalEarned : 0) / totalPossible * 100);
        return new ResLessonSection(LessonSectionType.SPEAKING, "SPEAKING", Math.min(percent, 100), false);
    }
}
