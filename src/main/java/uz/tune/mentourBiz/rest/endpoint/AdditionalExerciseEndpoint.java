package uz.tune.mentourBiz.rest.endpoint;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.admin.req.ReqUpdateWordTranslation;
import uz.tune.mentourBiz.rest.admin.req.create.adminCreate.ReqCreateTask;
import uz.tune.mentourBiz.rest.admin.req.create.adminCreate.ReqCreateUnit;
import uz.tune.mentourBiz.rest.admin.req.create.adminCreate.ReqCreateVocabSet;
import uz.tune.mentourBiz.rest.admin.req.upd.*;
import uz.tune.mentourBiz.rest.admin.res.ResContentHeader;
import uz.tune.mentourBiz.rest.model.ResAttachmentModel;
import uz.tune.mentourBiz.rest.payload.ReqUpsertVocabWord;
import uz.tune.mentourBiz.rest.payload.req.ReqDetailedQuestionCreate;
import uz.tune.mentourBiz.rest.payload.req.combinedBody.ReqCombinedUnitExerciseTask;
import uz.tune.mentourBiz.rest.payload.res.ResBooks;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.exercise.ResVocabSet;
import uz.tune.mentourBiz.rest.payload.res.lesson.ResUnit;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResAdminQuestions;
import uz.tune.mentourBiz.rest.payload.studentRes.res.exercise.ResVocabLearnWord;
import uz.tune.mentourBiz.rest.service.AdditionalExerciseService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + "/additional-content")
@RequiredArgsConstructor
public class AdditionalExerciseEndpoint {

    private final AdditionalExerciseService service;


    @GetMapping("/management/tasks")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).TASKS_GET)")
    public ResponseEntity<List<ResContentHeader>> getTasks(@RequestParam UUID unitUuid) {
        return ResponseEntity.ok(service.getTasksForManagement(unitUuid));
    }

    @GetMapping("/management/questions")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).QUESTIONS_GET)")
    public ResponseEntity<List<ResAdminQuestions>> getQuestions(@RequestParam UUID taskUuid) {
        return ResponseEntity.ok(service.getQuestionsForManagement(taskUuid));
    }

    @PostMapping(value = "/upload-listening", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).LISTENING_UPLOAD)")
    public ResponseEntity<ResAttachmentModel> uploadListening(
            @RequestParam("file") MultipartFile file,
            @RequestParam("bookUuid") UUID bookUuid) {
        return ResponseEntity.ok(service.uploadListeningFile(file, bookUuid));
    }

    @GetMapping("/vocab-sets/{setUuid}/words")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).WORDS_BY_SET_GET)")
    public ResponseEntity<List<ResVocabLearnWord>> getWordsBySet(@PathVariable UUID setUuid) {
        return ResponseEntity.ok(service.getWordsBySetUuid(setUuid));
    }

    @PatchMapping("/vocab-words/bulk-translations")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).BULK_UPDATE_TRANSLATIONS)")
    public ResponseEntity<ResponseMessage> bulkUpdateTranslations(@RequestBody List<ReqUpdateWordTranslation> requests) {
        return ResponseEntity.ok(service.bulkUpdateWordTranslations(requests));
    }

    @GetMapping("/management/sets")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).VOCAB_SETS_GET)")
    public ResponseEntity<List<ResContentHeader>> getVocabSets(@RequestParam UUID unitUuid) {
        return ResponseEntity.ok(service.getVocabTasksForManagement(unitUuid));
    }

    @GetMapping("/management/vocab-questions")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).VOCAB_QUESTIONS_GET)")
    public ResponseEntity<List<ResVocabLearnWord>> getVocabQuestions(@RequestParam UUID taskUuid) {
        return ResponseEntity.ok(service.getVocabQuestionsForManagement(taskUuid));
    }

    @PatchMapping("/questions/{uuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).SCHOOL_QUESTION_UPDATE)")
    public ResponseEntity<ResponseMessage> updateSchoolQuestion(
            @PathVariable UUID uuid,
            @RequestBody ReqUpdateQuestion req) {
        return ResponseEntity.ok(service.updateSchoolQuestion(uuid, req));
    }

    @GetMapping("/management/books")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).BOOKS_FOR_SELECTION_GET)")
    public ResponseEntity<List<ResBooks>> getBooksForSelection(
            @RequestParam(required = false) Boolean isGlobal,
            @RequestParam(required = false) UUID schoolUuid) {
        return ResponseEntity.ok(service.getAvailableBooks(isGlobal, schoolUuid));
    }

    @GetMapping("/management/units")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).UNITS_FOR_SELECTION_GET)")
    public ResponseEntity<List<ResUnit>> getUnitsForSelection(@RequestParam UUID bookUuid) {
        return ResponseEntity.ok(service.getUnits(bookUuid));
    }

    @GetMapping("/questions/{uuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).QUESTION_GET)")
    public ResponseEntity<ResAdminQuestions> getQuestion(@PathVariable UUID uuid) {
        return ResponseEntity.ok(service.getQuestion(uuid));
    }

    @GetMapping("/vocab-words/{uuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).VOCAB_WORD_GET)")
    public ResponseEntity<ResVocabLearnWord> getVocabWord(@PathVariable UUID uuid) {
        return ResponseEntity.ok(service.getVocabWord(uuid));
    }

    // book
    @PostMapping("/books")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).BOOK_CREATE)")
    public ResponseEntity<UUID> createBook(
            @RequestParam String name,
            @RequestParam UUID levelUuid,
            @RequestParam(required = false) UUID schoolId) {
        return ResponseEntity.ok(service.createBook(name, levelUuid, schoolId));
    }

    @PatchMapping("/books/{uuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).BOOK_UPDATE)")
    public ResponseEntity<ResponseMessage> updateBook(@PathVariable UUID uuid, @RequestBody ReqUpdateBook req) {
        return ResponseEntity.ok(service.updateBook(uuid, req));
    }


    // units
    @PostMapping("/units")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).UNIT_CREATE)")
    public ResponseEntity<UUID> createUnit(@RequestBody ReqCreateUnit req) {
        return ResponseEntity.ok(service.createUnit(req));
    }

    @PostMapping("/units/combined")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).COMBINED_UNIT)")
    public ResponseEntity<ResponseMessage> combinedUnit(@RequestBody ReqCombinedUnitExerciseTask req) {
        return ResponseEntity.ok(service.createCombinedUnit(req));
    }

    @PatchMapping("/units/{uuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).UNIT_UPDATE)")
    public ResponseEntity<ResponseMessage> updateUnit(@PathVariable UUID uuid, @RequestBody ReqUpdateUnit req) {
        service.updateUnit(uuid, req);
        return ResponseEntity.ok(new ResponseMessage("Unit updated successfully"));
    }

    @DeleteMapping("/units/{uuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).REMOVE_UNIT)")
    public ResponseEntity<ResponseMessage> removeUnit(@PathVariable UUID uuid) {
        service.deleteUnit(uuid);
        return ResponseEntity.ok(new ResponseMessage("Unit deactivated successfully"));
    }

    //  tasks
    @PostMapping("/tasks")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).TASK_CREATE)")
    public ResponseEntity<UUID> createTask(@RequestBody ReqCreateTask req) {
        return ResponseEntity.ok(service.createTask(req));
    }

    @PostMapping("/vocab-tasks")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).VOCAB_TASK_CREATE)")
    public ResponseEntity<List<ResVocabSet>> createVocabTask(@RequestBody List<ReqCreateVocabSet> reqVs) {
        return ResponseEntity.ok(service.createVocabTask(reqVs));
    }

    @PatchMapping("/tasks/vocab/{uuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).VOCAB_TASK_UPDATE)")
    public ResponseEntity<ResponseMessage> updateVocabTask(@PathVariable UUID uuid, @RequestBody ReqUpdateVocabSet req) {
        service.updateVocabTask(uuid, req);
        return ResponseEntity.ok(new ResponseMessage("Task updated successfully"));
    }

    @DeleteMapping("/tasks/vocab/{uuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).REMOVE_VOCAB_TASK)")
    public ResponseEntity<ResponseMessage> removeVocabTask(@PathVariable UUID uuid) {
        service.deleteVocabTask(uuid);
        return ResponseEntity.ok(new ResponseMessage("Task deactivated successfully"));
    }


    @PatchMapping("/tasks/{uuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).TASK_UPDATE)")
    public ResponseEntity<ResponseMessage> updateTask(@PathVariable UUID uuid, @RequestBody ReqUpdateTask req) {
        service.updateTask(uuid, req);
        return ResponseEntity.ok(new ResponseMessage("Task updated successfully"));
    }

    @DeleteMapping("/tasks/{uuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).REMOVE_TASK)")
    public ResponseEntity<ResponseMessage> removeTask(@PathVariable UUID uuid) {
        service.deleteTask(uuid);
        return ResponseEntity.ok(new ResponseMessage("Task deactivated successfully"));
    }

    @PostMapping("/vocab-words/upsert")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).VOCAB_WORD_UPSERT)")
    public ResponseEntity<UUID> upsertVocabWord(@RequestBody ReqUpsertVocabWord req) {
        return ResponseEntity.ok(service.upsertVocabWord(req));
    }

    @DeleteMapping("/vocab-sets/{setUuid}/words/{wordUuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).REMOVE_VOCAB_WORD)")
    public ResponseEntity<ResponseMessage> removeVocabWord(@PathVariable UUID setUuid, @PathVariable UUID wordUuid) {
        service.deleteVocabWord(wordUuid, setUuid);
        return ResponseEntity.ok(new ResponseMessage("Word unlinked from set"));
    }


    // qs
    @PostMapping("/questions")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).ADD_QUESTION)")
    public ResponseEntity<ResponseMessage> addQuestion(@RequestBody List<ReqDetailedQuestionCreate> req) {
        service.addQuestionsToTask(req);
        return ResponseEntity.ok(new ResponseMessage("Question added to task"));
    }

    @PatchMapping("/update/questions/{uuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).QUESTION_UPDATE)")
    public ResponseEntity<ResponseMessage> updateQuestion(@PathVariable UUID uuid, @RequestBody ReqUpdateQuestion req) {
        return ResponseEntity.ok(service.updateQuestion(uuid, req));
    }

    @DeleteMapping("/tasks/{taskUuid}/questions/{questionUuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).REMOVE_QUESTION)")
    public ResponseEntity<ResponseMessage> removeQuestion(@PathVariable UUID taskUuid, @PathVariable UUID questionUuid) {
        service.removeQuestionFromTask(taskUuid, questionUuid);
        return ResponseEntity.ok(new ResponseMessage("Question unlinked from task"));
    }
}