package uz.tune.mentourBiz.rest.endpoint.school.group;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.payload.req.schedule.ReqGroupSchedule;
import uz.tune.mentourBiz.rest.payload.req.schedule.ReqMarkAttendance;
import uz.tune.mentourBiz.rest.payload.req.schedule.ReqScheduleUpdate;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.school.group.schedule.ResGroupSchedule;
import uz.tune.mentourBiz.rest.payload.res.school.group.schedule.attendance.ResAttendanceRecord;
import uz.tune.mentourBiz.rest.service.group.schedule.ScheduleService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + "/schedules")
@RequiredArgsConstructor
public class ScheduleEndpoint {

    private final ScheduleService scheduleService;

    @PostMapping
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).SCHEDULE_CREATE)")
    public ResponseEntity<ResponseMessage> createSchedule(@RequestBody ReqGroupSchedule request) {
        return ResponseEntity.ok(scheduleService.createSchedule(request));
    }

    @PutMapping()
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).SCHEDULE_UPDATE)")
    public ResponseEntity<ResponseMessage> updateSchedule(@RequestBody ReqScheduleUpdate request) {
        return ResponseEntity.ok(scheduleService.updateSchedule(request));
    }

    @DeleteMapping("/{scheduleId}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).SCHEDULE_DELETE)")
    public ResponseEntity<ResponseMessage> deleteSchedule(@PathVariable UUID scheduleId) {
        return ResponseEntity.ok(scheduleService.deleteSchedule(scheduleId));
    }

    @GetMapping("/group/{groupId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ResGroupSchedule>> getSchedulesForGroup(
            @PathVariable UUID groupId,
            @PageableDefault(sort = "dueDate") Pageable pageable) {
        return ResponseEntity.ok(scheduleService.getSchedulesForGroup(groupId, pageable));
    }

    @PutMapping("/mark/attendance")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).MARK_SINGLE_ATTENDANCE)")
    public ResponseEntity<ResponseMessage> markSingleAttendance(
            @RequestBody ReqMarkAttendance request) {
        return ResponseEntity.ok(scheduleService.markSingleAttendance(request));
    }

    @GetMapping("/{lessonUuid}/attendance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ResAttendanceRecord>> getAttendanceForSchedule(@PathVariable UUID lessonUuid) {
        return ResponseEntity.ok(scheduleService.getAttendanceForSchedule(lessonUuid));
    }

    /**
     * Set (or clear, when teacherUuid is omitted) the teacher who actually conducted a lesson, so a
     * substituted lesson is credited to the substitute in teacher payroll.
     */
    @PatchMapping("/lesson/{lessonUuid}/teacher")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN','SCHOOL_ADMIN')")
    public ResponseEntity<ResponseMessage> assignLessonTeacher(
            @PathVariable UUID lessonUuid,
            @RequestParam(required = false) UUID teacherUuid) {
        return ResponseEntity.ok(scheduleService.assignLessonTeacher(lessonUuid, teacherUuid));
    }
}