package uz.tune.mentourBiz.rest.endpoint.school.group;


import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.enums.EnrollmentStatus;
import uz.tune.mentourBiz.rest.enums.GroupStatus;
import uz.tune.mentourBiz.rest.payload.req.group.ReqGroupCreate;
import uz.tune.mentourBiz.rest.payload.req.group.ReqGroupUpdate;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.level.ResLevel;
import uz.tune.mentourBiz.rest.payload.res.school.ResReferralLink;
import uz.tune.mentourBiz.rest.payload.res.school.group.ResGroup;
import uz.tune.mentourBiz.rest.payload.res.school.group.ResGroupBalance;
import uz.tune.mentourBiz.rest.payload.res.school.group.enrollment.ResGroupStudent;
import uz.tune.mentourBiz.rest.repository.group.LevelRepository;
import uz.tune.mentourBiz.rest.service.group.GroupService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + BaseURI.GROUPS)
@RequiredArgsConstructor
public class GroupEndpoint {

    private final GroupService groupService;
    private final LevelRepository levelRepository;

    @PostMapping
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).GROUP_CREATE)")
    public ResponseEntity<ResponseMessage> createGroup(@RequestBody ReqGroupCreate request) {
        return ResponseEntity.ok(groupService.createGroup(request));
    }

    @GetMapping("/school/{schoolUuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).GROUPS_BY_SCHOOL_GET)")
    public ResponseEntity<List<ResGroup>> getGroupsBySchool(@PathVariable UUID schoolUuid) {
        return ResponseEntity.ok(groupService.getGroupsBySchool(schoolUuid));
    }

    @GetMapping("/school/{schoolUuid}/balances")
//    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).GROUPS_BY_SCHOOL_GET)")
    public ResponseEntity<List<ResGroupBalance>> getGroupBalancesBySchool(
            @PathVariable UUID schoolUuid,
            @RequestParam(required = false) String groupName,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false, defaultValue = "false") Boolean onlyOverdue) {
        return ResponseEntity.ok(groupService.getGroupBalancesBySchool(schoolUuid, groupName, teacherName, onlyOverdue));
    }

    @PatchMapping("/{groupId}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).GROUP_UPDATE)")
    public ResponseEntity<ResponseMessage> updateGroup(@PathVariable UUID groupId, @RequestBody ReqGroupUpdate request) {
        return ResponseEntity.ok(groupService.updateGroup(groupId, request));
    }

    @GetMapping(BaseURI.LEVELS)
    public ResponseEntity<List<ResLevel>> getLevels(@RequestParam(required = false, defaultValue = "1") Long subjectId) {
        return ResponseEntity.ok(levelRepository.findAllBySubject_Id(subjectId, Sort.by(Sort.Direction.ASC, "sortOrder"))
                .stream().map(ResLevel::new).toList());
    }

    @DeleteMapping("/{groupId}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).GROUP_DELETE)")
    public ResponseEntity<ResponseMessage> deleteGroup(@PathVariable UUID groupId) {
        return ResponseEntity.ok(groupService.deleteGroup(groupId));
    }

    @PatchMapping(BaseURI.UNDELETE + "/{groupId}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).UN_DELETE_GROUP)")
    public ResponseEntity<ResponseMessage> unDeleteGroup(@PathVariable UUID groupId) {
        return ResponseEntity.ok(groupService.unDeleteGroup(groupId));
    }

    @GetMapping("/{groupId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResGroup> getGroup(@PathVariable UUID groupId) {
        return ResponseEntity.ok(groupService.getGroup(groupId));
    }

    @GetMapping("/{groupId}/students")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).ENROLLMENTS_GET)")
    public ResponseEntity<Page<ResGroupStudent>> getGroupStudents(
            @PathVariable UUID groupId,
            @RequestParam(required = false) String studentName,
            @RequestParam(required = false) EnrollmentStatus status,
            @RequestParam(required = false, defaultValue = "false") Boolean onlyOverdue,
            @RequestParam(required = false) UUID packageId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
            ) {
        return ResponseEntity.ok(groupService.getGroupStudents(page,size, groupId, studentName, status, onlyOverdue, packageId));
    }

    @PostMapping(BaseURI.REFERRAL + "/{groupId}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).REFERRAL_LINK_GENERATE)")
    public ResponseEntity<ResReferralLink> generateReferralLink(@PathVariable UUID groupId){
        return ResponseEntity.ok(groupService.generateReferralLink(groupId));
    }

    @GetMapping
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).GROUPS_GET)")
    public ResponseEntity<Page<ResGroup>> getGroups(
            @PageableDefault(sort = "name") Pageable pageable,
            @RequestParam(required = false) GroupStatus status,
            @RequestParam(required = false) UUID schoolId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String levelName,
            @RequestParam(required = false) String teacherName
    ) {
        return ResponseEntity.ok(groupService.getGroups(pageable, schoolId, branchId, status,
                className, levelName, teacherName));
    }
}