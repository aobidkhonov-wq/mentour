package uz.tune.mentourBiz.rest.admin.getAllNoLinks;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.admin.res.ResContentHeader;
import uz.tune.mentourBiz.rest.payload.res.ResBooks;
import uz.tune.mentourBiz.rest.payload.res.lesson.ResUnit;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResExerciseQuestion;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResVocabLearnWord;

@RestController
@RequestMapping(BaseURI.API1 + "/admin/management")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SYS_ADMIN')")
@Hidden
public class AdminContentManagementEndpoint {

    private final AdminContentManagementService managementService;

    @GetMapping("/books")
    public ResponseEntity<Page<ResBooks>> getBooks(
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(managementService.getAllBooks(pageable));
    }

    @GetMapping("/units")
    public ResponseEntity<Page<ResUnit>> getUnits(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(managementService.getAllUnits(pageable));
    }

    @GetMapping("/tasks")
    public ResponseEntity<Page<ResContentHeader>> getTasks(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(managementService.getAllTasks(pageable));
    }

    @GetMapping("/questions")
    public ResponseEntity<Page<ResExerciseQuestion>> getQuestions(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(managementService.getAllQuestions(pageable));
    }

    @GetMapping("/vocab-words")
    public ResponseEntity<Page<ResVocabLearnWord>> getWords(
            @PageableDefault(sort = "word", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(managementService.getAllWords(pageable));
    }

    @GetMapping("/vocab-sets")
    public ResponseEntity<Page<ResContentHeader>> getVocabSets(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(managementService.getAllVocabSets(pageable));
    }
}