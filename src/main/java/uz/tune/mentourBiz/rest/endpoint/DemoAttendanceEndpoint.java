package uz.tune.mentourBiz.rest.endpoint;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.payload.req.schedule.ReqMarkDemoAttendance;
import uz.tune.mentourBiz.rest.payload.res.demo.ResDemoAttendanceRecord;
import uz.tune.mentourBiz.rest.service.DemoAttendanceService;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(BaseURI.API1 + BaseURI.DEMO_ATTENDANCE)
@RequiredArgsConstructor
public class DemoAttendanceEndpoint {

    private final DemoAttendanceService demoAttendanceService;

    /** Returns demo attendance for multiple lessons, keyed by lessonUuid. */
    @GetMapping("/by-lessons")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).ATTENDANCE_GRID_GET) " +
            "or hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).MARK_SINGLE_ATTENDANCE)")
    public ResponseEntity<Map<String, List<ResDemoAttendanceRecord>>> getByLessons(
            @RequestParam List<String> lessonUuids) {
        List<UUID> uuids = lessonUuids.stream().map(UUID::fromString).collect(Collectors.toList());
        return ResponseEntity.ok(demoAttendanceService.getByLessons(uuids));
    }

    /** Returns all demo attendees for a lesson. */
    @GetMapping("/lesson/{lessonUuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).ATTENDANCE_GRID_GET) " +
            "or hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).MARK_SINGLE_ATTENDANCE)")
    public ResponseEntity<List<ResDemoAttendanceRecord>> getByLesson(@PathVariable UUID lessonUuid) {
        return ResponseEntity.ok(demoAttendanceService.getByLesson(lessonUuid));
    }

    /** Teacher marks PRESENT or ABSENT for a demo student — updates crm_leads.demo_status directly. */
    @PatchMapping("/lead/{crmLeadUuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).MARK_SINGLE_ATTENDANCE)")
    public ResponseEntity<ResDemoAttendanceRecord> mark(
            @PathVariable UUID crmLeadUuid,
            @Valid @RequestBody ReqMarkDemoAttendance req) {
        return ResponseEntity.ok(demoAttendanceService.mark(crmLeadUuid, req));
    }

}
