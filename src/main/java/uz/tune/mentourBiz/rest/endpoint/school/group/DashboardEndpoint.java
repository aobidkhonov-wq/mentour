package uz.tune.mentourBiz.rest.endpoint.school.group;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.enums.CourseStatus;
import uz.tune.mentourBiz.rest.enums.LessonStatus;
import uz.tune.mentourBiz.rest.payload.res.course.ResCourseList;
import uz.tune.mentourBiz.rest.payload.res.lesson.ResUpcomingLesson;
import uz.tune.mentourBiz.rest.service.lessons.CourseService;
import uz.tune.mentourBiz.rest.service.lessons.DashboardService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + BaseURI.DASHBOARD)
@RequiredArgsConstructor
public class DashboardEndpoint {

    private final DashboardService dashboardService;
    private final CourseService courseService;

    @GetMapping(BaseURI.LESSONS)
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).DASHBOARD_GET_LESSONS)")
    public ResponseEntity<Page<ResUpcomingLesson>> getUpcomingLessons(
            @RequestParam(name="size", defaultValue = "10") int size,
            @RequestParam(name="page", defaultValue = "0") int page,
            @RequestParam(required = false) List<LessonStatus> status,
            @RequestParam(required = false) UUID schoolId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime"));
        return ResponseEntity.ok(dashboardService.getUpcomingLessons(pageable, status, schoolId));
    }

    @GetMapping(BaseURI.COURSES)
    public ResponseEntity<Page<ResCourseList>> getAllCourses(@RequestParam(name="size", defaultValue = "10") int size,
                                                             @RequestParam(name="page", defaultValue = "0") int page,
                                                             @RequestParam(required = false) CourseStatus status,
                                                             @RequestParam(required = false) UUID schoolId,
                                                             @RequestParam(required = false) String courseName,
                                                             @RequestParam(required = false) String className,
                                                             @RequestParam(required = false) String bookName,
                                                             @RequestParam(required = false) UUID packageUuid ){
        Sort sortByCreatedArtDesc = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page, size,sortByCreatedArtDesc);
        return ResponseEntity.ok(courseService.getAllCourses(pageable, status, schoolId, courseName, className, bookName, packageUuid));
    }


}