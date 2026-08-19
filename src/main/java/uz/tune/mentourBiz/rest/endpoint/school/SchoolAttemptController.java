package uz.tune.mentourBiz.rest.endpoint.school;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.payload.req.attempt.ReqResetAttempt;
import uz.tune.mentourBiz.rest.payload.req.attempt.ReqSetAttemptLimit;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.attempt.ResAttemptStatus;
import uz.tune.mentourBiz.rest.payload.res.attempt.SchoolLimitAttempts;
import uz.tune.mentourBiz.rest.service.school.SchoolAttemptService;

import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + "/school/attempt")
@RequiredArgsConstructor
public class SchoolAttemptController {

    private final SchoolAttemptService schoolAttemptService;


    @GetMapping("/limit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SchoolLimitAttempts> getLimit(@RequestParam(required = false) UUID schoolUuid) {
        return ResponseEntity.ok(schoolAttemptService.getSchoolAttemptLimit(schoolUuid));
    }

    @PutMapping("/limit")
    @PreAuthorize("hasAnyAuthority('SCHOOL_ADMIN','SCHOOL_DIRECTOR','SYS_ADMIN')")
    public ResponseEntity<ResponseMessage> setLimit(@Valid @RequestBody ReqSetAttemptLimit request) {
        return ResponseEntity.ok(schoolAttemptService.setSchoolAttemptLimit(request));
    }


    @GetMapping("/status/{taskUuid}")
    @PreAuthorize("hasAuthority('STUDENT')")
    public ResponseEntity<ResAttemptStatus> getStatus(@PathVariable UUID taskUuid) {
        return ResponseEntity.ok(schoolAttemptService.getStatus(taskUuid,null));
    }

    @PostMapping("/consume/{taskUuid}")
    @PreAuthorize("hasAuthority('STUDENT')")
    public ResponseEntity<ResAttemptStatus> consume(@PathVariable UUID taskUuid) {
        return ResponseEntity.ok(schoolAttemptService.consumeAttempt(taskUuid));
    }


    @PostMapping("/reset")
    @PreAuthorize("hasAnyAuthority('TEACHER','SCHOOL_ADMIN','SCHOOL_DIRECTOR','SYS_ADMIN')")
    public ResponseEntity<ResAttemptStatus> reset(@Valid @RequestBody ReqResetAttempt request) {
        return ResponseEntity.ok(schoolAttemptService.resetAttempt(request));
    }
}
