package uz.tune.mentourBiz.rest.endpoint.users;
import uz.tune.mentourBiz.rest.enums.MessageKey;


import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.exception.PaymentRequiredException;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.enums.FinanceEnums;
import uz.tune.mentourBiz.rest.enums.UserStatus;
import uz.tune.mentourBiz.rest.payload.req.ReqAddParents;
import uz.tune.mentourBiz.rest.payload.req.ReqUpdateParent;
import uz.tune.mentourBiz.rest.payload.req.student.*;
import uz.tune.mentourBiz.rest.payload.res.student.ResBulkStudentResult;
import uz.tune.mentourBiz.rest.payload.req.user.ReqApproveUserList;
import uz.tune.mentourBiz.rest.payload.req.user.ReqDeclineUserList;
import uz.tune.mentourBiz.rest.payload.res.ResAtRiskStudent;
import uz.tune.mentourBiz.rest.payload.res.ResFinanceHistoryWrapper;
import uz.tune.mentourBiz.rest.payload.res.ResParentDetail;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.studentApp.ResStudentHomeProfile;
import uz.tune.mentourBiz.rest.payload.res.user.ResStudentForLesson;
import uz.tune.mentourBiz.rest.payload.res.user.ResStudentList;
import uz.tune.mentourBiz.rest.payload.res.user.ResStudentOne;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.service.ParentContractService;
import uz.tune.mentourBiz.rest.service.helper.AuthToViewEntity;
import uz.tune.mentourBiz.rest.service.user.StudentService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + BaseURI.STUDENT)
@RequiredArgsConstructor
public class StudentEndpoint {

    private final StudentService studentService;
    private final ParentContractService parentContractService;
    private final StudentRepo studentRepo;
    private final AuthToViewEntity authToViewEntity;

    @PostMapping("/{studentUuid}/activate-billing")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).BILLING_ACTIVATE)")
    public ResponseEntity<ResponseMessage> activateBilling(@PathVariable UUID studentUuid) {
        Student student = studentRepo.findByUuid(studentUuid)
                .orElseThrow(() -> new uz.tune.mentourBiz.exception.EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

        authToViewEntity.authorizeActionUponStudent(student);

        if (!student.getSchool().isPaymentActive()) {
            throw new PaymentRequiredException(MessageKey.BILLING_INACTIVE.getKey());
        }

        student.setPaymentActivated(true);
        studentRepo.save(student);
        return ResponseEntity.ok(new ResponseMessage("STUDENT_BILLING_ACTIVATED"));
    }

    @GetMapping("/at-risk")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).AT_RISK_STUDENTS_GET)")
    public ResponseEntity<Page<ResAtRiskStudent>> getAtRiskStudents(
            @RequestParam(required = false) UUID schoolUuid,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(studentService.getAtRiskStudents(schoolUuid, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).STUDENT_CREATE)")
    public ResponseEntity<ResponseMessage> createStudents(@RequestBody ReqStudentCreate student) {
        return ResponseEntity.ok(studentService.createStudents(student));
    }

    // Bulk-create students and enroll them into a group, scoped to the school in the path.
    @PostMapping("/bulk/{schoolId}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).STUDENT_CREATE)")
    public ResponseEntity<ResBulkStudentResult> createStudentsForGroup(
            @PathVariable UUID schoolId,
            @RequestBody List<ReqBulkStudent> requests) {
        return ResponseEntity.ok(studentService.createStudentsForGroup(schoolId, requests));
    }

    @PatchMapping("/de-refer")
    public ResponseEntity<ResponseMessage> deReferStudent(@RequestBody ReqStudentAssign reqStudentAssign) {
        return ResponseEntity.ok(studentService.deReferStudent(reqStudentAssign));
    }

    @GetMapping("/home/profile")
    public ResponseEntity<ResStudentHomeProfile> getStudentHomeProfile() {
        return ResponseEntity.ok(studentService.getStudentHomeProfile());
    }

    @PatchMapping(BaseURI.UPDATE + "/{uuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).STUDENT_UPDATE)")
    public ResponseEntity<ResponseMessage> updateStudent(@PathVariable UUID uuid, @RequestBody ReqStudentsUpdate reqStudentsUpdates){
        return ResponseEntity.ok(studentService.updateStudent(uuid,reqStudentsUpdates));
    }

    @PatchMapping(BaseURI.ASSIGN)
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).STUDENT_ASSIGN)")
    public ResponseEntity<ResponseMessage> assignStudentToClass(@RequestBody ReqStudentAssign reqStudentAssign){
        return ResponseEntity.ok(studentService.assignStudentsToClass(reqStudentAssign));
    }

    // Unassigned (ONGOING enrollmenti yo'q) studentlarni user uuid ro'yxati bo'yicha guruhga biriktiradi
    @PatchMapping(BaseURI.ASSIGN + "/unassigned")
    @PreAuthorize("hasAuthority('SCHOOL_ADMIN')")
    public ResponseEntity<ResponseMessage> assignUnassignedStudentsToGroup(@RequestBody ReqUnassignedStudentAssign req){
        return ResponseEntity.ok(studentService.assignUnassignedStudentsToGroup(req));
    }

    @PatchMapping(BaseURI.REMOVE)
    public ResponseEntity<ResponseMessage> deAssignStudentFromClass(@RequestBody ReqStudentAssign reqStudentAssign){
        return ResponseEntity.ok(studentService.deAssignStudentFromClass(reqStudentAssign));
    }

    @GetMapping(BaseURI.LIST)
    public ResponseEntity<Page<ResStudentList>> getAllStudents(
            @RequestParam(name="size", required = false) Integer size,
            @RequestParam(name="page", required = false) Integer page,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) UUID schoolId,
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) UUID courseId) {

        return ResponseEntity.ok(studentService.getAllStudents(page,size, status, fullName, schoolId, classId, courseId));
    }

    @GetMapping(BaseURI.BALANCE + "/{userUuid}")
    public ResponseEntity<ResFinanceHistoryWrapper> getStudentsPaymentHistory(@PathVariable(required = false) UUID userUuid, Pageable pageable, FinanceEnums.PaymentMethod method, Instant from, Instant to){
        return ResponseEntity.ok(studentService.getStudentsBalanceHistory(userUuid, pageable, method, from, to));
    }

    @GetMapping(BaseURI.BALANCE + "/{userUuid}/charges")
    public ResponseEntity<ResFinanceHistoryWrapper> getStudentsChargeHistory(
            @PathVariable UUID userUuid,
            @RequestParam(required = false) FinanceEnums.FinanceTransactionType type,
            @RequestParam(required = false) FinanceEnums.PaymentMethod method,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to){
        ZoneId zone = ZoneId.of("Asia/Tashkent");
        Instant fromInstant = (from != null) ? from.atStartOfDay(zone).toInstant() : null;
        Instant toInstant = (to != null) ? to.atTime(23, 59, 59, 999_999_999).atZone(zone).toInstant() : null;
        return ResponseEntity.ok(studentService.getStudentsHistoryByType(userUuid, type, method, pageable, fromInstant, toInstant));
    }

    @GetMapping(BaseURI.CLASSES + "/{classId}")
    public ResponseEntity<List<ResStudentList>> getStudentsForClass(
            @PathVariable UUID classId,
            @RequestParam(required = false) String fullName) {
        return ResponseEntity.ok(studentService.getStudentForClass(classId, fullName));
    }


    @PostMapping("/parents/batch")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).ADD_PARENTS_BATCH)")
    public ResponseEntity<ResponseMessage> addParentsBatch(@RequestBody ReqAddParents req) {
        return ResponseEntity.ok(parentContractService.addParentsBatch(req));
    }

    @GetMapping("/{studentUuid}/parents")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).PARENTS_GET)")
    public ResponseEntity<List<ResParentDetail>> getParents(@PathVariable UUID studentUuid) {
        return ResponseEntity.ok(parentContractService.getParentsByStudent(studentUuid));
    }

    @PatchMapping("/parent-contact/{contactUuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).PARENT_UPDATE)")
    public ResponseEntity<ResParentDetail> updateParent(
            @PathVariable UUID contactUuid,
            @RequestBody ReqUpdateParent req) {
        return ResponseEntity.ok(parentContractService.updateParent(contactUuid, req));
    }

    @DeleteMapping("/parent-contact/{contactUuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).PARENT_DELETE)")
    public ResponseEntity<ResponseMessage> deleteParent(@PathVariable UUID contactUuid) {
        return ResponseEntity.ok(parentContractService.removeParentLink(contactUuid));
    }

    @DeleteMapping(BaseURI.DELETE + "/{userUuid}")
    public ResponseEntity<ResponseMessage> deleteStudent(
            @PathVariable UUID userUuid,
            @RequestParam(required = false) String note) {
        return ResponseEntity.ok(studentService.deleteStudent(userUuid, note));
    }

    @GetMapping(BaseURI.SCHOOLS + BaseURI.CLASSES + "/{groupUuid}")
    public ResponseEntity<List<ResStudentList>> getSchoolStudentsToAddToClass(@PathVariable UUID groupUuid) {
        return ResponseEntity.ok(studentService.getSchoolStudentsToAddToClass(groupUuid));
    }

    @GetMapping(BaseURI.ALL)
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).STUDENT_GET_ALL)")
    public ResponseEntity<List<ResStudentList>> getAllStudentsList(@RequestParam UserStatus status) {
        return ResponseEntity.ok(studentService.getAllStudents(status));
    }

    @GetMapping("/referred/{classId}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).VIEW_REFERRED_STUDENTS)")
    public ResponseEntity<List<ResStudentList>> getReferredStudents(@PathVariable UUID classId) {
        return ResponseEntity.ok(studentService.getReferredStudents(classId));
    }

    @PatchMapping("/approve")
    public ResponseEntity<ResponseMessage> approveStudent(@RequestBody ReqApproveUserList reqApproveUserList) {
        return ResponseEntity.ok(studentService.approveStudents(reqApproveUserList));
    }

    @PostMapping("/decline")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).DECLINE_REFERRED_STUDENT)")
    public ResponseEntity<ResponseMessage> declineStudents(@RequestBody ReqDeclineUserList reqDeclineUserList) {
        return ResponseEntity.ok(studentService.declineStudents(reqDeclineUserList));
    }


    @GetMapping(BaseURI.ONE + "/{uuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).STUDENT_GET_ONE)")
    public ResponseEntity<ResStudentOne> getStudent(@PathVariable UUID uuid) {
        return ResponseEntity.ok(studentService.getStudentByUuid(uuid));
    }

    @GetMapping(BaseURI.LESSONS + "/{lessonId}")
    public ResponseEntity<List<ResStudentForLesson>> getStudentsForLessonForBbb(@PathVariable UUID lessonId) {
        return ResponseEntity.ok(studentService.getStudentsForLesson(lessonId));
    }


    @PostMapping("/transfer")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).STUDENTS_TRANSFER)")
    public ResponseEntity<ResponseMessage> transferStudents(@RequestParam UUID studentUuid,
                                                            @RequestParam UUID targetGroupUuid,
                                                            @RequestParam(defaultValue = "false") boolean withBillingPlan) {
        return ResponseEntity.ok(studentService.transferStudent(studentUuid, targetGroupUuid, withBillingPlan));
    }




}
