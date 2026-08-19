package uz.tune.mentourBiz.rest.admin.get;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.admin.res.ResContentHeader;
import uz.tune.mentourBiz.rest.enums.LessonSectionType;
import uz.tune.mentourBiz.rest.payload.res.ResBooks;
import uz.tune.mentourBiz.rest.payload.res.lesson.ResUnit;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResAdminQuestions;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResExerciseQuestion;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResVocabLearnWord;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + "/admin/content")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SYS_ADMIN')")
public class AdminContentEndpointGet {

    private final AdminContentServiceGet adminContentService;

    @GetMapping("/books")
    public ResponseEntity<List<ResBooks>> getBooks() {
        return ResponseEntity.ok(adminContentService.getBooks());
    }

    @GetMapping("/words/by/units/{uuid}")
    public ResponseEntity<List<ResVocabLearnWord>> getWordByUnits(@PathVariable UUID uuid) {
        return ResponseEntity.ok(adminContentService.getWordsBySetUnitUuid(uuid));
    }

    @GetMapping("/units")
    public ResponseEntity<List<ResUnit>> getUnits(@RequestParam UUID bookUuid) {
        return ResponseEntity.ok(adminContentService.getUnits(bookUuid));
    }

    @GetMapping("/tasks")
    public ResponseEntity<List<ResContentHeader>> getTasks(@RequestParam UUID unitUuid, @RequestParam LessonSectionType type) {
        if (type == LessonSectionType.VOCABULARY) {
            return ResponseEntity.ok(adminContentService.getVocabSets(unitUuid));
        }
        return ResponseEntity.ok(adminContentService.getTasks(unitUuid, type));
    }

    @GetMapping("/questions")
    public ResponseEntity<List<ResExerciseQuestion>> getQuestions(@RequestParam UUID taskUuid) {
        return ResponseEntity.ok(adminContentService.getQuestions(taskUuid));
    }

    @GetMapping("/questions/{uuid}")
    public ResponseEntity<ResAdminQuestions> getQuestion(@PathVariable UUID uuid) {
        return ResponseEntity.ok(adminContentService.getQuestionByUuid(uuid));
    }

    @GetMapping("/vocab-words/{uuid}")
    public ResponseEntity<ResVocabLearnWord> getVocabWord(@PathVariable UUID uuid) {
        return ResponseEntity.ok(adminContentService.getVocabWordByUuid(uuid));
    }

    @GetMapping("/vocab-details")
    public ResponseEntity<List<ResVocabLearnWord>> getVocab(@RequestParam UUID setUuid) {
        return ResponseEntity.ok(adminContentService.getVocabWords(setUuid));
    }
}