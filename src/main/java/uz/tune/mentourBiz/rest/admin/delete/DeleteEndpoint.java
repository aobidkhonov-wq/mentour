package uz.tune.mentourBiz.rest.admin.delete;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;

import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + "/admin/content")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SYS_ADMIN')")
public class DeleteEndpoint {

    private final AdminContentServiceDelete adminContentService;

    @DeleteMapping("/books/{uuid}")
    public ResponseEntity<ResponseMessage> deleteBook(@PathVariable UUID uuid) {
        return ResponseEntity.ok(adminContentService.deleteBook(uuid));
    }

    @DeleteMapping("/units/{uuid}")
    public ResponseEntity<ResponseMessage> deleteUnit(@PathVariable UUID uuid) {
        return ResponseEntity.ok(adminContentService.deleteUnit(uuid));
    }

    @DeleteMapping("/tasks/{uuid}")
    public ResponseEntity<ResponseMessage> deleteTask(@PathVariable UUID uuid) {
        return ResponseEntity.ok(adminContentService.deleteTask(uuid));
    }

    @DeleteMapping("/questions/{uuid}")
    public ResponseEntity<ResponseMessage> deleteQuestion(@PathVariable UUID uuid) {
        return ResponseEntity.ok(adminContentService.deleteQuestion(uuid));
    }

    @DeleteMapping("/vocab-sets/{uuid}")
    public ResponseEntity<ResponseMessage> deleteVocabSet(@PathVariable UUID uuid) {
        return ResponseEntity.ok(adminContentService.deleteVocabSet(uuid));
    }

    @DeleteMapping("/vocab-words/{uuid}")
    public ResponseEntity<ResponseMessage> deleteVocabWord(@PathVariable UUID uuid) {
        return ResponseEntity.ok(adminContentService.deleteVocabWord(uuid));
    }
}
