package uz.tune.mentourBiz.rest.endpoint.exercise;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.enums.LessonSectionType;
import uz.tune.mentourBiz.rest.payload.req.exercise.ReqManualGrade;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.studentReq.req.exercise.ReqExerciseSubmit;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResGradingResult;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResQuestionsTasks;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResTaskResult;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResUnitTaskInfos;
import uz.tune.mentourBiz.rest.service.exercise.ExerciseService;

import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + BaseURI.EXERCISE)
@RequiredArgsConstructor
public class ExerciseEndpoint {

    private final ExerciseService exerciseService;

    @GetMapping(BaseURI.LIST + "/{unitId}")
    public ResponseEntity<ResUnitTaskInfos> getExerciseForUnit(
            @PathVariable UUID unitId,
            @RequestParam(name = "type") LessonSectionType type
    ) {
        return ResponseEntity.ok(exerciseService.getExerciseForUnit(unitId, type));
    }

    @GetMapping(BaseURI.SUMMARY+ "/{studentUuid}" + "/{unitId}")
    public ResponseEntity<ResUnitTaskInfos> getExerciseForUnit(
            @PathVariable UUID studentUuid,
            @PathVariable UUID unitId,
            @RequestParam(name = "type") LessonSectionType type
    ) {
        return ResponseEntity.ok(exerciseService.getExercisesForStudent(studentUuid, unitId, type));
    }

    @GetMapping("{taskUuid}/questions")
    public ResponseEntity<ResQuestionsTasks> getQuestionsForTask(@PathVariable UUID taskUuid) {
        return ResponseEntity.ok(exerciseService.getQuestionsForTask(taskUuid));
    }
    @PostMapping("/{questionUuid}/submit")
    public ResponseEntity<ResGradingResult> submitAnswer(
            @PathVariable UUID questionUuid,
            @RequestBody ReqExerciseSubmit request) {
        return ResponseEntity.ok(exerciseService.grade(questionUuid, request));
    }

    @GetMapping("/{taskUuid}/result")
    public ResponseEntity<ResTaskResult> getTaskResult(@PathVariable UUID taskUuid, @RequestParam(required = false) Boolean flag) {
        return ResponseEntity.ok(exerciseService.getTaskResult(taskUuid, flag));
    }


    @GetMapping("/{taskUuid}/result/{studentUuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).TASK_RESULT_FOR_STUDENT_GET)")
    public ResponseEntity<ResTaskResult> getTaskResultForStudent(
            @PathVariable UUID taskUuid,
            @PathVariable UUID studentUuid, @RequestParam(required = false) Boolean flag) {
        return ResponseEntity.ok(exerciseService.getTaskResultForStudent(studentUuid, taskUuid, flag));
    }

    @PostMapping("/{unitUuid}/grade/{studentUuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).MANUAL_GRADE)")
    public ResponseEntity<ResponseMessage> manualGradeUnit(
            @PathVariable UUID unitUuid,
            @PathVariable UUID studentUuid,
            @RequestBody ReqManualGrade request) {
        return ResponseEntity.ok(exerciseService.manualGradeUnit(studentUuid, unitUuid, request));
    }

}