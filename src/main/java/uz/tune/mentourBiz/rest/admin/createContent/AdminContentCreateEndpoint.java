package uz.tune.mentourBiz.rest.admin.createContent;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.admin.createContent.createContent.AdminBookDuplicationService;
import uz.tune.mentourBiz.rest.admin.createContent.createContent.AdminContentServiceCreateImpl;
import uz.tune.mentourBiz.rest.admin.req.create.adminCreate.*;
import uz.tune.mentourBiz.rest.domain.Message;
import uz.tune.mentourBiz.rest.payload.req.ReqUpsertMessage;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.repository.MessageRepo;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + "/admin/content")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SYS_ADMIN')")
public class AdminContentCreateEndpoint {

    private final AdminContentServiceCreateImpl adminContentService;
    private final AdminBookDuplicationService duplicationService;
    private final MessageRepo messageRepo;

    @PostMapping("/books")
    public ResponseEntity<ResponseMessage> createBook(@RequestBody List<ReqCreateBook> req) {
        return ResponseEntity.ok(adminContentService.createBook(req));
    }

    @PostMapping("/books/duplicate/{bookUuid}")
    public ResponseEntity<ResponseMessage> duplicateBook(@PathVariable UUID bookUuid) {
        return ResponseEntity.ok(duplicationService.duplicateBook(bookUuid));
    }

    @PostMapping("/templates/upsert")
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    public ResponseEntity<ResponseMessage> upsertTemplate(@RequestBody ReqUpsertMessage req) {
        Message msg = messageRepo.findTopByKeyAndLang(req.getKey(), req.getLang())
                .orElse(new Message());
        msg.setKey(req.getKey());
        msg.setLang(req.getLang());
        msg.setMessage(req.getTemplate());
        messageRepo.save(msg);
        return ResponseEntity.ok(new ResponseMessage("Template updated"));
    }

    @PostMapping("/units")
    public ResponseEntity<ResponseMessage> createUnit(@RequestBody List<ReqCreateUnit> req) {
        return ResponseEntity.ok(adminContentService.createUnit(req));
    }

    @PostMapping("/tasks")
    public ResponseEntity<ResponseMessage> createTask(@RequestBody List<ReqCreateTask> req) {
        return ResponseEntity.ok(adminContentService.createTask(req));
    }

    @PostMapping("/questions")
    public ResponseEntity<ResponseMessage> createQuestion(@RequestBody List<ReqCreateQuestion> req) {
        return ResponseEntity.ok(adminContentService.createQuestion(req));
    }

    @PostMapping("/vocab-sets")
    public ResponseEntity<ResponseMessage> createVocabSet(@RequestBody List<ReqCreateVocabSet> req) {
        return ResponseEntity.ok(adminContentService.createVocabSet(req));
    }

    @PostMapping("/vocab-words")
    public ResponseEntity<ResponseMessage> createVocabWord(@RequestBody List<ReqCreateVocabWord> req) {
        return ResponseEntity.ok(adminContentService.createVocabWord(req));
    }

    @PostMapping("/vocab-link")
    public ResponseEntity<ResponseMessage> linkWord(@RequestBody List<ReqLinkWordToSet> req) {
        return ResponseEntity.ok(adminContentService.linkWordToSet(req));
    }
}