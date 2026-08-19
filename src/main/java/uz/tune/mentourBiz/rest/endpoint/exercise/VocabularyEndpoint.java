package uz.tune.mentourBiz.rest.endpoint.exercise;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.payload.res.exercise.ResVocabSet;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResVocabGradingResult;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResVocabLearnWord;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResVocabQuizWord;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResVocabSetResult;
import uz.tune.mentourBiz.rest.service.vocabulary.VocabularyService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + BaseURI.VOCAB)
@RequiredArgsConstructor
public class VocabularyEndpoint {
    private final VocabularyService vocabularyService;

    @GetMapping("/unit/{unitId}/sets")
    public ResponseEntity<List<ResVocabSet>> getSets(@PathVariable UUID unitId) {
        return ResponseEntity.ok(vocabularyService.getVocabularySetsForUnit(unitId));
    }

    @GetMapping("/set/{setUuid}/learn")
    public ResponseEntity<List<ResVocabLearnWord>> getLearnWords(@PathVariable UUID setUuid) {
        return ResponseEntity.ok(vocabularyService.getLearnWordsForSet(setUuid));
    }


    @GetMapping("/set/{setUuid}/quiz")
    public ResponseEntity<List<ResVocabQuizWord>> getQuizWords(@PathVariable UUID setUuid) {
        return ResponseEntity.ok(vocabularyService.getQuizWordsForSet(setUuid));
    }

    @GetMapping("/set/{setUuid}/result")
    public ResponseEntity<ResVocabSetResult> getSetResult(@PathVariable UUID setUuid, @RequestParam(required = false) Boolean flag) {
        return ResponseEntity.ok(vocabularyService.getVocabularySetResult(setUuid,flag));
    }

    @GetMapping("/set/{setUuid}/result/{studentUuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).VOCAB_RESULT_FOR_STUDENT_GET)")
    public ResponseEntity<ResVocabSetResult> getVocabResultForStudent(
            @PathVariable UUID setUuid,
            @PathVariable UUID studentUuid,  @RequestParam(required = false) Boolean flag) {
        return ResponseEntity.ok(vocabularyService.getVocabResultForStudent(studentUuid, setUuid,flag));
    }

    @PostMapping("/submit/{wordUuid}")
    public ResponseEntity<ResVocabGradingResult> submitVocabAnswer(
            @PathVariable UUID wordUuid,
            @RequestParam String answer, @RequestParam UUID setUuid) {
        return ResponseEntity.ok(vocabularyService.gradeVocabulary(wordUuid, answer, setUuid));
    }

}