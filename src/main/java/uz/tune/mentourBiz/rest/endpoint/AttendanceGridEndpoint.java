package uz.tune.mentourBiz.rest.endpoint;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.payload.res.ResAttendanceGrid;
import uz.tune.mentourBiz.rest.payload.res.ResOverallAttendance;
import uz.tune.mentourBiz.rest.service.AttendanceGridService;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + "/attendance")
@RequiredArgsConstructor
public class AttendanceGridEndpoint {

    private final AttendanceGridService attendanceGridService;

    @GetMapping("/{courseUuid}")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).ATTENDANCE_GRID_GET)")
    public ResponseEntity<ResAttendanceGrid> getAttendanceGrid(
            @PathVariable UUID courseUuid,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ResponseEntity.ok(attendanceGridService.getMonthlyGrid(courseUuid, year, month));
    }

    @GetMapping("/overall")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).ATTENDANCE_GRID_GET)")
    public ResponseEntity<ResOverallAttendance> getOverallAttendance(
            @RequestParam UUID groupUuid,
            @RequestParam UUID studentUuid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(attendanceGridService.getOverallAttendance(groupUuid, studentUuid, date));
    }
}
