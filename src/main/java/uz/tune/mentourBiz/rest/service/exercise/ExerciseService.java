package uz.tune.mentourBiz.rest.service.exercise;

import uz.tune.mentourBiz.rest.enums.LessonSectionType;
import uz.tune.mentourBiz.rest.payload.req.exercise.ReqManualGrade;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.studentReq.req.exercise.ReqExerciseSubmit;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResGradingResult;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResQuestionsTasks;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResTaskResult;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResUnitTaskInfos;

import java.util.UUID;

public interface ExerciseService {
    ResGradingResult grade(UUID questionUuid, ReqExerciseSubmit request);
    ResQuestionsTasks getQuestionsForTask(UUID taskUuid);
    ResTaskResult getTaskResult(UUID taskUuid, Boolean flag);
    ResTaskResult getTaskResultForStudent(UUID studentUuid, UUID taskUuid, Boolean flag);
    ResUnitTaskInfos getExerciseForUnit(UUID unitId, LessonSectionType type);

    ResUnitTaskInfos getExercisesForStudent(UUID studentUuid, UUID unitId, LessonSectionType type);

    ResponseMessage manualGradeUnit(UUID studentUuid, UUID unitUuid, ReqManualGrade request);
}
