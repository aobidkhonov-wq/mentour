package uz.tune.mentourBiz.rest.endpoint.school.course;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.enums.CourseStatus;
import uz.tune.mentourBiz.rest.payload.ResCourseGroupDetails;
import uz.tune.mentourBiz.rest.payload.req.course.ReqCourse;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.course.ResCourseList;
import uz.tune.mentourBiz.rest.payload.res.course.ResCourseView;
import uz.tune.mentourBiz.rest.payload.res.course.ResCreateCourse;
import uz.tune.mentourBiz.rest.service.lessons.CourseService;

import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + BaseURI.COURSES)
@RequiredArgsConstructor
public class CourseEndpoint {

    private final CourseService courseService;


    @GetMapping
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).COURSE_GET_ALL)")
    public ResponseEntity<Page<ResCourseList>> getAllCourses(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) CourseStatus status,
            @RequestParam(required = false) UUID schoolId,
            @RequestParam(required = false) String courseName,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String bookName, @RequestParam(required = false) UUID packageUuid) {

        return ResponseEntity.ok(courseService.getAllCourses(pageable, status, schoolId, courseName, className, bookName, packageUuid));
    }

    @GetMapping("/group/{groupUuid}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ResCourseList>> getCoursesByGroup(
            @PathVariable UUID groupUuid,
            @RequestParam(required = false) CourseStatus status,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(courseService.getCoursesByGroup(groupUuid, status, pageable));
    }

    @GetMapping("/{courseId}/group-details")
    public ResponseEntity<ResCourseGroupDetails> getCourseGroupDetails(@PathVariable UUID courseId) {
        return ResponseEntity.ok(courseService.getCourseGroupDetails(courseId));
    }


    @PostMapping("/{courseId}/finish")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).COURSE_FINISH)")
    public ResponseEntity<ResponseMessage> finishCourse(@PathVariable UUID courseId,
                                                        @RequestParam(required = false) String endDate) {
        return ResponseEntity.ok(courseService.finishCourse(courseId,endDate));
    }


    @GetMapping("/{courseId}")
    public ResponseEntity<ResCourseView> getCourseById(@PathVariable UUID courseId) {
        return ResponseEntity.ok(courseService.getCourseById(courseId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).COURSE_CREATE)")
    public ResponseEntity<ResCreateCourse> createCourse(@RequestBody ReqCourse request) {
        return ResponseEntity.ok(courseService.createCourse(request));
    }

    @PatchMapping("/{courseId}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).COURSE_UPDATE)")
    public ResponseEntity<ResponseMessage> updateCourse(@PathVariable UUID courseId, @RequestBody ReqCourse request) {
        return ResponseEntity.ok(courseService.updateCourse(courseId, request));
    }

    @DeleteMapping("/{courseId}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).COURSE_DELETE)")
    public ResponseEntity<ResponseMessage> deleteCourse(@PathVariable UUID courseId) {
        return ResponseEntity.ok(courseService.deleteCourse(courseId));
    }

    @DeleteMapping("/{courseId}" + BaseURI.PERMANENT)
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).DELETE_COURSE_HARD)")
    public ResponseEntity<ResponseMessage> hardDeleteCourse(@PathVariable UUID courseId) {
        return ResponseEntity.ok(courseService.hardDeleteCourse(courseId));
    }

    @PatchMapping("/{courseId}" + BaseURI.UNDELETE)
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).COURSE_UNDELETE)")
    public ResponseEntity<ResponseMessage> undeleteCourse(@PathVariable UUID courseId) {
        return ResponseEntity.ok(courseService.undeleteCourse(courseId));
    }

}