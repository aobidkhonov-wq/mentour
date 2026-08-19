package uz.tune.mentourBiz.rest.endpoint.subject;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.payload.req.subject.ReqSubject;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.subject.ResSubject;
import uz.tune.mentourBiz.rest.service.subject.SubjectService;

import java.util.List;

@RestController
@RequestMapping(BaseURI.API1 + BaseURI.SUBJECTS)
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @PostMapping
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    public ResponseEntity<ResponseMessage> createSubject(@RequestBody ReqSubject request) {
        return ResponseEntity.ok(subjectService.createSubject(request));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ResSubject>> getSubjects() {
        return ResponseEntity.ok(subjectService.getSubjects());
    }

    @GetMapping("/{subjectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResSubject> getSubject(@PathVariable Long subjectId) {
        return ResponseEntity.ok(subjectService.getSubject(subjectId));
    }

    @PatchMapping("/{subjectId}")
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    public ResponseEntity<ResponseMessage> updateSubject(@PathVariable Long subjectId, @RequestBody ReqSubject request) {
        return ResponseEntity.ok(subjectService.updateSubject(subjectId, request));
    }

    @DeleteMapping("/{subjectId}")
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    public ResponseEntity<ResponseMessage> deleteSubject(@PathVariable Long subjectId) {
        return ResponseEntity.ok(subjectService.deleteSubject(subjectId));
    }
}
