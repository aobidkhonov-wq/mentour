package uz.tune.mentourBiz.rest.endpoint.exercise.answeres;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.answers.ResVocabPreviewAnswers;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.answers.ResWritingPreviewAnswers;
import uz.tune.mentourBiz.rest.service.exerciseAnswers.StudentAnswersService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + BaseURI.ANSWERS)
@RequiredArgsConstructor
public class StudentAnswersEndpoint {

    private final StudentAnswersService studentAnswersService;


    //fixme: first get the tasks then questions
    @GetMapping(BaseURI.WRITING)
    public ResponseEntity<List<ResWritingPreviewAnswers>> getWritingTaskForUnitPreview(@RequestParam UUID unitUuid){
        return ResponseEntity.ok(studentAnswersService.getWritingTaskForUnitPreview(unitUuid));
    }

    @GetMapping(BaseURI.VOCAB)
    public ResponseEntity<List<ResVocabPreviewAnswers>> getVocabularyForUnitPreview(@RequestParam UUID unitUuid){
        return ResponseEntity.ok(studentAnswersService.getVocabTasksForUnitPreview(unitUuid));
    }

//    @GetMapping()
//    public ResExerciseAnswers getExerciseAnswersForStudentByUnit(@RequestParam(name = "studentUUId") UUID studnetUUId,
//                                                                 @RequestParam(name = "unitUuid") UUID unitUUId){
//
//    }

}
