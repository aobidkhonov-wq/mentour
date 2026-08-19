package uz.tune.mentourBiz.rest.endpoint.school.group;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.payload.req.enrollment.ReqAssignBillingPlan;
import uz.tune.mentourBiz.rest.payload.req.enrollment.ReqEnrollmentCreate;
import uz.tune.mentourBiz.rest.payload.req.enrollment.ReqEnrollmentStatusUpdate;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.school.group.enrollment.ResEnrollment;
import uz.tune.mentourBiz.rest.service.group.enrollment.EnrollmentService;


import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + "/enrollments")
@RequiredArgsConstructor
public class EnrollmentEndpoint {

    //FIXME if else checks

    private final EnrollmentService enrollmentService;

    @PostMapping
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).ENROLLMENTS_CREATE)")
    public ResponseEntity<ResponseMessage> createEnrollments(@RequestBody ReqEnrollmentCreate request) {
        return ResponseEntity.ok(enrollmentService.createEnrollments(request));
    }

    @PostMapping("/assign-billing-plan")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).ENROLLMENTS_CREATE)")
    public ResponseEntity<ResponseMessage> assignBillingPlan(
            @RequestParam UUID groupUuid,
            @RequestBody ReqAssignBillingPlan request) {
        return ResponseEntity.ok(enrollmentService.assignBillingPlan(groupUuid, request));
    }

    @PatchMapping("/{enrollmentId}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).ENROLLMENT_STATUS_UPDATE)")
    public ResponseEntity<ResponseMessage> updateEnrollmentStatus(
            @PathVariable UUID enrollmentId,
            @RequestBody ReqEnrollmentStatusUpdate request) {
        return ResponseEntity.ok(enrollmentService.updateEnrollmentStatus(enrollmentId, request));
    }

    @PatchMapping("/{enrollmentId}/restore-lesson/{lessonId}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).ENROLLMENT_STATUS_UPDATE)")
    public ResponseEntity<ResponseMessage> restoreLesson(
            @PathVariable UUID enrollmentId,
            @PathVariable UUID lessonId) {
        return ResponseEntity.ok(enrollmentService.restoreLesson(enrollmentId, lessonId));
    }

    @GetMapping
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).ENROLLMENTS_GET)")
    public ResponseEntity<Page<ResEnrollment>> getEnrollments(
            @PageableDefault(sort = "id") Pageable pageable,
            @RequestParam(required = false) UUID groupId,
            @RequestParam(required = false) UUID studentId
    ) {
        return ResponseEntity.ok(enrollmentService.getEnrollments(pageable, groupId, studentId));
    }
}