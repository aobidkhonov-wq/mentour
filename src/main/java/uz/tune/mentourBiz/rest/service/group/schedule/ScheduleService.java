package uz.tune.mentourBiz.rest.service.group.schedule;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.payload.req.schedule.ReqGroupSchedule;
import uz.tune.mentourBiz.rest.payload.req.schedule.ReqMarkAttendance;
import uz.tune.mentourBiz.rest.payload.req.schedule.ReqScheduleUpdate;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.school.group.schedule.ResGroupSchedule;
import uz.tune.mentourBiz.rest.payload.res.school.group.schedule.attendance.ResAttendanceRecord;

import java.util.List;
import java.util.UUID;

public interface ScheduleService {
    ResponseMessage createSchedule(ReqGroupSchedule request);
    ResponseMessage updateSchedule(ReqScheduleUpdate request);
    ResponseMessage deleteSchedule(UUID scheduleId);
    Page<ResGroupSchedule> getSchedulesForGroup(UUID groupId, Pageable pageable);
    ResponseMessage markSingleAttendance(ReqMarkAttendance request);
    ResponseMessage assignLessonTeacher(UUID lessonUuid, UUID teacherUuid);
    List<ResAttendanceRecord> getAttendanceForSchedule(UUID lessonUuid);
    void createUnitsProgressesForNewSchedule(UUID groupUuid, List<Unit> units);
}