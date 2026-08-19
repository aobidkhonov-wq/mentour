package uz.tune.mentourBiz.rest.service.exercise;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.rest.enums.StudentSummaryMetric;
import uz.tune.mentourBiz.rest.payload.req.ReqGetUnits;
import uz.tune.mentourBiz.rest.payload.res.ResLastAttendance;
import uz.tune.mentourBiz.rest.payload.res.ResStudentDashboardSummary;
import uz.tune.mentourBiz.rest.payload.res.lesson.ResActiveHomework;
import uz.tune.mentourBiz.rest.payload.res.lesson.ResGroupActiveHomework;
import uz.tune.mentourBiz.rest.payload.res.lesson.ResUnit;
import uz.tune.mentourBiz.rest.payload.res.school.group.ResGroup;
import uz.tune.mentourBiz.rest.payload.res.user.ResStudentCount;
import uz.tune.mentourBiz.rest.payload.res.user.ResStudentList;
import uz.tune.mentourBiz.rest.payload.studentRes.res.lesson.ResLesson;
import uz.tune.mentourBiz.rest.payload.studentRes.res.lesson.ResLessonUnits;

import java.util.List;
import java.util.UUID;

public interface StudentLessonService {
    List<ResLesson> getUnitsForCurrentUser(UUID groupUuid);
    List<ResLessonUnits> getUnitsGroupedByLesson(UUID groupUuid);
    ResActiveHomework getOneUnit(UUID unitUuid);
    List<ResLesson> getUnitsForCurrentUserV2();

    List<ResUnit> getAvailableUnits(ReqGetUnits reqGetUnits);
    List<ResUnit> getUnitsForGroupByCourse(UUID courseUuid, UUID bookUuid);
    ResActiveHomework getActiveHomework(UUID groupUuid);
    List<ResGroupActiveHomework> getActiveHomeworkV2(UUID groupUuid);

    List<ResGroup> getMyGroups();
    ResLastAttendance getLastAttendanceStatus();
    ResStudentDashboardSummary getStudentSummary(UUID schoolUuid);
    ResActiveHomework getUnitSummaryForStudent(UUID studentUuid, UUID unitUuid);

    Page<ResStudentList> getStudentSummaryDetails(UUID schoolUuid, StudentSummaryMetric metric, Pageable pageable);

    List<ResStudentCount> getStudentsCount(UUID schoolUuid);

}