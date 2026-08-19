package uz.tune.mentourBiz.rest.endpoint.school;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.MessageKey;
import uz.tune.mentourBiz.rest.enums.SchoolStatus;
import uz.tune.mentourBiz.rest.payload.req.ReqSchoolExamSettings;
import uz.tune.mentourBiz.rest.payload.req.school.ReqSchoolCreate;
import uz.tune.mentourBiz.rest.payload.req.school.ReqSchoolUpdate;
import uz.tune.mentourBiz.rest.payload.req.school.ReqUpdateMentorHours;
import uz.tune.mentourBiz.rest.payload.res.ResSchoolExamSettings;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.school.ResMentorsForSchool;
import uz.tune.mentourBiz.rest.payload.res.school.ResSchoolInfo;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.service.school.SchoolService;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + BaseURI.SCHOOLS)
@RequiredArgsConstructor
public class SchoolEndpoint {

    private final SchoolService schoolService;
    private final SchoolRepo schoolRepo;
    private final UserService userService;
    private final StudentRepo studentRepo;
    private final UserScopeService userScopeService;

    @GetMapping
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).SCHOOL_GET_ALL)")
    public ResponseEntity<Page<ResSchoolInfo>> getAllSchools(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) SchoolStatus status,
            @RequestParam(required = false) String schoolName) {

        return ResponseEntity.ok(schoolService.getAllSchools(pageable, status, schoolName));
    }

    @GetMapping("/director/my-schools")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).DIRECTOR_SCHOOLS_GET)")
    public ResponseEntity<List<ResSchoolInfo>> getDirectorSchools() {
        return ResponseEntity.ok(schoolService.getDirectorSchools());
    }


    @GetMapping(BaseURI.ALL)
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).SCHOOL_GET_ALL)")
    public ResponseEntity<List<ResSchoolInfo>> getAllSchools() {;
        return ResponseEntity.ok(schoolService.getAllSchoolsList());
    }

    @PostMapping("/backfill-defaults")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).BACKFILL_DEFAULTS)")
    public ResponseEntity<ResponseMessage> backfillDefaults() {
        return ResponseEntity.ok(schoolService.backfillDefaults());
    }

    @GetMapping("/{schoolId}")
    public ResponseEntity<ResSchoolInfo> getSchoolById(@PathVariable UUID schoolId) {
        return ResponseEntity.ok(schoolService.getSchoolById(schoolId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).SCHOOL_CREATE)")
    public ResponseEntity<ResponseMessage> createSchool(@RequestBody ReqSchoolCreate request) {
        return ResponseEntity.ok(schoolService.createSchool(request));
    }


    @GetMapping("/{schoolId}/exam-settings")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).EXAM_SETTINGS_GET)")
    public ResponseEntity<ResSchoolExamSettings> getExamSettings(@PathVariable UUID schoolId) {
        return ResponseEntity.ok(schoolService.getExamSettings(schoolId));
    }

    @PostMapping("/{schoolId}/exam-settings")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).EXAM_SETTINGS_UPDATE)")
    public ResponseEntity<ResponseMessage> updateExamSettings(
            @PathVariable UUID schoolId,
            @RequestBody ReqSchoolExamSettings request) {
        return ResponseEntity.ok(schoolService.updateExamSettings(schoolId, request));
    }

    @PostMapping("/{schoolId}/activate-payment")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).PAYMENT_ACTIVATE)")
    public ResponseEntity<ResponseMessage> activatePayment(@PathVariable UUID schoolId) {
        User user = userService.getCurrentUser();

        UUID targetSchoolUuid = userScopeService.resolveSchoolUuid(schoolId);

        School school = schoolRepo.findByUuid(targetSchoolUuid)
                .orElseThrow(() -> new uz.tune.mentourBiz.exception.EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));

        if (school.isPaymentActive()) throw new ValidationException("ALREADY_ACTIVE");

        school.setPaymentActive(true);
        school.setPaymentActivatedTime(Instant.now());
        school.setPaymentActivatedBy(user);
        schoolRepo.save(school);

        studentRepo.findAllBySchool_Uuid(targetSchoolUuid, Pageable.unpaged()).forEach(s -> {
            s.setPaymentActivated(true);
            studentRepo.save(s);
        });
        return ResponseEntity.ok(new ResponseMessage("SCHOOL_PAYMENT_ACTIVATED"));
    }

    @PatchMapping("/{schoolId}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).SCHOOL_UPDATE)")
    public ResponseEntity<ResponseMessage> updateSchool(@PathVariable UUID schoolId, @RequestBody ReqSchoolUpdate request) {
        return ResponseEntity.ok(schoolService.updateSchool(schoolId, request));
    }

    @DeleteMapping("/{schoolId}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).SCHOOL_DELETE)")
    public ResponseEntity<ResponseMessage> deleteSchool(@PathVariable UUID schoolId) {
        return ResponseEntity.ok(schoolService.deleteSchool(schoolId));
    }

    @DeleteMapping("/{schoolId}" + BaseURI.PERMANENT)
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).SCHOOL_HARD_DELETE)")
    public ResponseEntity<ResponseMessage> hardDeleteSchool(@PathVariable UUID schoolId) {
        return ResponseEntity.ok(schoolService.hardDeleteSchool(schoolId));
    }

    @PatchMapping("/{schoolId}" + BaseURI.UNDELETE)
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).SCHOOL_UNDELETE)")
    public ResponseEntity<ResponseMessage> undeleteSchool(@PathVariable UUID schoolId) {
        return ResponseEntity.ok(schoolService.undeleteSchool(schoolId));
    }




    @GetMapping(BaseURI.MENTOR + "/{schoolId}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).SCHOOL_GET_MENTORS)")
    public ResponseEntity<Page<ResMentorsForSchool>> getMentorsForSchool(@PathVariable UUID schoolId, @RequestParam(name="size", defaultValue = "10") int size,
                                                                         @RequestParam(name="page", defaultValue = "0") int page) {
        Sort sortByCreatedArtDesc = Sort.by(Sort.Direction.DESC, "contractHours");
        Pageable pageable = PageRequest.of(page, size,sortByCreatedArtDesc);
        return ResponseEntity.ok(schoolService.getMentorsForSchool(schoolId, pageable));
    }

    @PutMapping(BaseURI.MENTOR + "/{schoolMentorId}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).SCHOOL_UPDATE_MENTOR_ASSIGNMENT)")
    public ResponseEntity<ResponseMessage> updateMentorHours(
            @PathVariable UUID schoolMentorId,
            @RequestBody ReqUpdateMentorHours request) {
        return ResponseEntity.ok(schoolService.updateMentorHours(schoolMentorId, request));
    }

    @DeleteMapping(BaseURI.MENTOR  + "/{schoolMentorId}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).REMOVE_MENTOR_FROM_SCHOOL)")
    public ResponseEntity<ResponseMessage> removeMentorFromSchool(@PathVariable UUID schoolMentorId) {
        return ResponseEntity.ok(schoolService.removeMentorFromSchool(schoolMentorId));
    }
}