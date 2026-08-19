package uz.tune.mentourBiz.rest.endpoint.school.course;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.enums.LessonStatus;
import uz.tune.mentourBiz.rest.payload.req.course.ReqLesson;
import uz.tune.mentourBiz.rest.payload.req.course.ReqLessonUnitAssignment;
import uz.tune.mentourBiz.rest.payload.req.course.ReqLessonUpdateItem;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.course.ResLessonDetail;
import uz.tune.mentourBiz.rest.payload.res.lesson.ResCreateLessons;
import uz.tune.mentourBiz.rest.payload.res.lesson.ResStartMeeting;
import uz.tune.mentourBiz.rest.service.lessons.CourseLessonService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1)
@RequiredArgsConstructor
public class CourseLessonEndpoint {

    private final CourseLessonService courseStudentLessonService;


    @PostMapping(BaseURI.COURSES + "/{courseId}" + BaseURI.LESSONS)
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).LESSONS_CREATE)")
    public ResponseEntity<ResCreateLessons> createLessons(
            @PathVariable UUID courseId,
            @RequestBody List<ReqLesson> requestList
    ) {
        return ResponseEntity.ok(courseStudentLessonService.createLessons(courseId, requestList));
    }



    @GetMapping(BaseURI.COURSES + "/{courseId}" + BaseURI.LESSONS)
    public ResponseEntity<Page<ResLessonDetail>> getLessonsForCourse(
            @PathVariable UUID courseId,
            @RequestParam(required = false) List<LessonStatus> status,
            @RequestParam(required = false) String lessonName,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date, // Yangi
            @RequestParam(name="size", defaultValue = "10") int size,
            @RequestParam(name="page", defaultValue = "0") int page) {

        Sort sortByDate = Sort.by(Sort.Direction.DESC, "startTime");
        Pageable pageable = PageRequest.of(page, size, sortByDate);

        return ResponseEntity.ok(courseStudentLessonService.getLessonsForCourse(courseId, pageable, status, lessonName, date));
    }


    @GetMapping(BaseURI.LESSONS + "/{lessonId}")
    public ResponseEntity<ResLessonDetail> getLessonById(@PathVariable UUID lessonId) {
        return ResponseEntity.ok(courseStudentLessonService.getLessonById(lessonId));
    }

    @DeleteMapping(BaseURI.LESSONS + "/{lessonId}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).LESSON_DELETE)")
    public ResponseEntity<ResponseMessage> deleteLesson(@PathVariable UUID lessonId) {
        return ResponseEntity.ok(courseStudentLessonService.deleteLesson(lessonId));
    }

    @PatchMapping(BaseURI.LESSONS + "/{lessonId}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).UN_DELETE_LESSON)")
    public ResponseEntity<ResponseMessage> unDeleteLesson(@PathVariable UUID lessonId) {
        return ResponseEntity.ok(courseStudentLessonService.undeleteLesson(lessonId));
    }

    @PatchMapping(BaseURI.COURSES + "/{courseId}" + BaseURI.LESSONS)
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).LESSONS_UPDATE)")
    public ResponseEntity<ResponseMessage> updateLessons(
            @PathVariable UUID courseId,
            @RequestBody List<ReqLessonUpdateItem> requestList) {
        return ResponseEntity.ok(courseStudentLessonService.updateLessons(courseId, requestList));
    }




    @PatchMapping(BaseURI.GROUPS + "/{groupId}/books/{schoolBookId}" + BaseURI.LESSONS + BaseURI.UNITS)
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).LESSONS_UPDATE)")
    public ResponseEntity<ResponseMessage> assignBookUnitsToLessons(
            @PathVariable UUID groupId,
            @PathVariable UUID schoolBookId,
            @RequestBody List<ReqLessonUnitAssignment> assignments) {
        return ResponseEntity.ok(courseStudentLessonService.assignBookUnitsToLessons(groupId, schoolBookId, assignments));
    }


    @PostMapping(BaseURI.LESSONS + "/{lessonId}" + BaseURI.START + BaseURI.MEETING)

    public ResponseEntity<ResStartMeeting> startLessonMeeting(@PathVariable UUID lessonId) {
        return ResponseEntity.ok(courseStudentLessonService.startLessonMeeting(lessonId));
    }
}