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
public class ExercisesHandler implements SectionHandler {

    private final ExerciseTaskRepository taskRepo;
    private final ExerciseAnswersRepository answerRepo;
    private final ExerciseQuestionRepository questionRepo;
    private final RewardCalculationService rewardCalculationService; // Add this

    @Override
    public ResLessonSection getSectionProgress(Student student, Unit unit) {
        List<ExerciseTask> tasks = taskRepo.findAllByUnit_UuidAndSectionTypeAndStatus(unit.getUuid(), LessonSectionType.GRAMMAR, ExerciseTaskStatus.ACTIVE);
        if (tasks.isEmpty()) return null;

        long totalPossible = 0;
        for (ExerciseTask task : tasks) {
            List<ExerciseQuestion> questions = questionRepo.findAllByExerciseTask_Uuid(task.getUuid());
            for (ExerciseQuestion q : questions) {
                totalPossible += rewardCalculationService.getDynamicExerciseReward(q, student.getSchool().getUuid());
            }
        }

        if (totalPossible == 0) return new ResLessonSection(LessonSectionType.GRAMMAR, "GRAMMAR", 0, false);

        long totalEarned = answerRepo.sumTotalEarnedScoreByStudentAndUnitAndSectionType(
                student.getId(), unit.getUuid(), LessonSectionType.GRAMMAR);

        int percent = (int) Math.round((double) totalEarned / totalPossible * 100);
        return new ResLessonSection(LessonSectionType.GRAMMAR, "GRAMMAR", Math.min(percent, 100), false);
    }
}