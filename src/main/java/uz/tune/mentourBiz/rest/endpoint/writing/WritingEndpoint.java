package uz.tune.mentourBiz.rest.endpoint.writing;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.enums.ExerciseSubmissionStatus;
import uz.tune.mentourBiz.rest.payload.req.writing.ReqTeacherFeedBackForWriting;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.exercise.ResWriting;
import uz.tune.mentourBiz.rest.payload.res.lesson.ResWritingList;
import uz.tune.mentourBiz.rest.service.writing.WritingService;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping(BaseURI.API1 + BaseURI.WRITING)
public class WritingEndpoint {


    private final WritingService writingService;

//    @GetMapping(BaseURI.STUDENT)
//    public ResponseEntity<ResWriting> getWritingForStudent(UUID studentUuid, UUID questionUuid){
//        return ResponseEntity.ok(writingService.getStudentsWriting(studentUuid, questionUuid));
//    }

    @GetMapping("/{submissionUuid}")
    public ResponseEntity<ResWriting> getWritingSubmission(@PathVariable UUID submissionUuid) {
        return ResponseEntity.ok(writingService.getWritingSubmission(submissionUuid));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).ALL_SUBMISSIONS_GET)")
    public ResponseEntity<Page<ResWritingList>> getAllSubmissions(
            @RequestParam(required = false) ExerciseSubmissionStatus status,
            @RequestParam(required = false) UUID studentUuid,
            @RequestParam(required = false) UUID groupUuid,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(writingService.getAllSubmissions(status, studentUuid, groupUuid, pageable));
    }

    @PostMapping("/{submissionUuid}/reject")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).SUBMISSION_REJECT)")
    public ResponseEntity<ResponseMessage> rejectSubmission(@PathVariable UUID submissionUuid) {
        return ResponseEntity.ok(writingService.rejectSubmission(submissionUuid));
    }

    @PostMapping(BaseURI.CHECK + "/{submissionUUID}")
    public ResponseEntity<ResponseMessage> checkWriting(@PathVariable UUID submissionUUID,
                                                        @RequestBody ReqTeacherFeedBackForWriting reqTeacherFeedBackForWriting) {
        return ResponseEntity.ok(writingService.gradeAndGiveFeedback(submissionUUID, reqTeacherFeedBackForWriting));
    }

    @PostMapping("/{submissionUuid}/ai-check")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).TRIGGER_AI_CHECK)")
    public ResponseEntity<ResponseMessage> triggerAiCheck(@PathVariable UUID submissionUuid) {
        return ResponseEntity.ok(writingService.triggerAiReview(submissionUuid));
    }

}
