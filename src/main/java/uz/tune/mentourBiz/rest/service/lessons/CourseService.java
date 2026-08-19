package uz.tune.mentourBiz.rest.service.lessons;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uz.tune.mentourBiz.rest.enums.CourseStatus;
import uz.tune.mentourBiz.rest.payload.ResCourseGroupDetails;
import uz.tune.mentourBiz.rest.payload.req.course.ReqCourse;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.course.ResCourseList;
import uz.tune.mentourBiz.rest.payload.res.course.ResCourseView;
import uz.tune.mentourBiz.rest.payload.res.course.ResCreateCourse;

import java.util.UUID;

public interface CourseService {
    Page<ResCourseList> getAllCourses(Pageable pageable, CourseStatus status, UUID schoolId,
                                      String courseName, String className, String bookName, UUID packageUuid);
    ResCourseView getCourseById(UUID courseId);
    // Course dashboard built for a specific student (used by the parent dashboard). No caller-based auth.
    ResCourseView getCourseViewForStudent(UUID courseId, UUID studentUserUuid);
    ResCreateCourse createCourse(ReqCourse request);
    ResponseMessage updateCourse(UUID courseId, ReqCourse request);
    ResponseMessage deleteCourse(UUID courseId);
    ResponseMessage hardDeleteCourse(UUID courseId);
    ResponseMessage undeleteCourse(UUID courseId);
    ResponseMessage finishCourse(UUID courseId,String endDate);
    Page<ResCourseList> getCoursesByGroup(UUID groupUuid, CourseStatus status, Pageable pageable);
    ResCourseGroupDetails getCourseGroupDetails(UUID courseId);
}