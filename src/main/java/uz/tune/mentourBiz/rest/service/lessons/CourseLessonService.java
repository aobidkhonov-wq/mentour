package uz.tune.mentourBiz.rest.service.lessons;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uz.tune.mentourBiz.rest.enums.LessonStatus;
import uz.tune.mentourBiz.rest.payload.req.course.ReqLesson;
import uz.tune.mentourBiz.rest.payload.req.course.ReqLessonUnitAssignment;
import uz.tune.mentourBiz.rest.payload.req.course.ReqLessonUpdateItem;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.course.ResLessonDetail;
import uz.tune.mentourBiz.rest.payload.res.lesson.ResCreateLessons;
import uz.tune.mentourBiz.rest.payload.res.lesson.ResStartMeeting;

import java.util.List;
import java.util.UUID;

public interface CourseLessonService {
    Page<ResLessonDetail> getLessonsForCourse(UUID courseId, Pageable pageable,
                                              List<LessonStatus> lessonStatus,
                                              String lessonName,
                                              java.time.LocalDate date);
    ResLessonDetail getLessonById(UUID lessonId);
    ResCreateLessons createLessons(UUID courseId, List<ReqLesson> requestList);
    ResponseMessage updateLessons(UUID courseId, List<ReqLessonUpdateItem> requestList);
    ResponseMessage assignBookUnitsToLessons(UUID groupId, UUID schoolBookId, List<ReqLessonUnitAssignment> assignments);
    ResponseMessage deleteLesson(UUID lessonId);
    ResponseMessage undeleteLesson(UUID lessonUuid);
    ResStartMeeting startLessonMeeting(UUID lessonId);
}