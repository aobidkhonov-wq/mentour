package uz.tune.mentourBiz.rest.admin.edit;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.admin.req.upd.*;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;

import java.util.UUID;


@RestController
@RequestMapping(BaseURI.API1 + "/admin/content")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SYS_ADMIN')")
public class AdminContentEditEndpoint {

    private final AdminContentServiceEdit adminContentService;

    @PatchMapping("/books/{uuid}")
    public ResponseEntity<ResponseMessage> updateBook(@PathVariable UUID uuid, @RequestBody ReqUpdateBook req) {
        return ResponseEntity.ok(adminContentService.updateBook(uuid, req));
    }

    @PatchMapping("/units/{uuid}")
    public ResponseEntity<ResponseMessage> updateUnit(@PathVariable UUID uuid, @RequestBody ReqUpdateUnit req) {
        return ResponseEntity.ok(adminContentService.updateUnit(uuid, req));
    }

    @PatchMapping("/tasks/{uuid}")
    public ResponseEntity<ResponseMessage> updateTask(@PathVariable UUID uuid, @RequestBody ReqUpdateTask req) {
        return ResponseEntity.ok(adminContentService.updateTask(uuid, req));
    }

    @PatchMapping("/questions/{uuid}")
    public ResponseEntity<ResponseMessage> updateQuestion(@PathVariable UUID uuid, @RequestBody ReqUpdateQuestion req) {
        return ResponseEntity.ok(adminContentService.updateQuestion(uuid, req));
    }

    @PatchMapping("/vocab-words/{uuid}")
    public ResponseEntity<ResponseMessage> updateVocabWord(@PathVariable UUID uuid, @RequestBody ReqUpdateVocabWord req) {
        return ResponseEntity.ok(adminContentService.updateVocabWord(uuid, req));
    }

    @PatchMapping("/vocab-sets/{uuid}")
    public ResponseEntity<ResponseMessage> updateVocabSet(@PathVariable UUID uuid, @RequestBody ReqUpdateVocabSet req) {
        return ResponseEntity.ok(adminContentService.updateVocabSet(uuid, req));
    }

    @DeleteMapping("/vocab-link/{questionUuid}")
    public ResponseEntity<ResponseMessage> unlinkWord(@PathVariable UUID questionUuid) {
        return ResponseEntity.ok(adminContentService.unlinkWordFromSet(questionUuid));
    }
}
