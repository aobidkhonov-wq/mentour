package uz.tune.mentourBiz.rest.endpoint.studentApp;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.payload.req.ReqGetUnits;
import uz.tune.mentourBiz.rest.payload.res.ResLastAttendance;
import uz.tune.mentourBiz.rest.payload.res.lesson.ResActiveHomework;
import uz.tune.mentourBiz.rest.payload.res.lesson.ResUnit;
import uz.tune.mentourBiz.rest.payload.res.school.group.ResGroup;
import uz.tune.mentourBiz.rest.payload.studentRes.res.lesson.ResLesson;
import uz.tune.mentourBiz.rest.payload.studentRes.res.lesson.ResLessonUnits;
import uz.tune.mentourBiz.rest.service.exercise.StudentLessonService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + BaseURI.UNITS)
@RequiredArgsConstructor
public class StudentLessonEndpoint {

    private final StudentLessonService lessonService;

    @GetMapping(BaseURI.STUDENT)
    public ResponseEntity<List<ResLesson>> getUnits(@RequestParam(name = "groupUuid", required = false) UUID groupUuid) {
        return ResponseEntity.ok(lessonService.getUnitsForCurrentUser(groupUuid));
    }

    @GetMapping(BaseURI.STUDENT + "/by-lesson")
    public ResponseEntity<List<ResLessonUnits>> getUnitsGroupedByLesson(@RequestParam(name = "groupUuid", required = false) UUID groupUuid) {
        return ResponseEntity.ok(lessonService.getUnitsGroupedByLesson(groupUuid));
    }

    @GetMapping(BaseURI.GROUPS + BaseURI.MY)
    public ResponseEntity<List<ResGroup>> getMyGroups() {
        return ResponseEntity.ok(lessonService.getMyGroups());
    }

    @GetMapping("/{unitUUID}")
    public ResponseEntity<ResActiveHomework> getOneUnit(@PathVariable UUID unitUUID) {
        return ResponseEntity.ok(lessonService.getOneUnit(unitUUID));
    }
    @GetMapping("/active-homework")
    public ResponseEntity<ResActiveHomework> getActiveHomework(@RequestParam(name = "groupUuid", required = false) UUID groupUuid) {
        return ResponseEntity.ok(lessonService.getActiveHomework(groupUuid));
    }
    @GetMapping("/last-attendance")
    public ResponseEntity<ResLastAttendance> getLastAttendance() {
        return ResponseEntity.ok(lessonService.getLastAttendanceStatus());
    }
    @GetMapping(BaseURI.GROUPS + "/{courseUuid}")
    public ResponseEntity<List<ResUnit>> getUnitsForGroupByCourse(@PathVariable UUID courseUuid, @RequestParam UUID bookUuid) {
        return ResponseEntity.ok(lessonService.getUnitsForGroupByCourse(courseUuid, bookUuid));
    }

    @GetMapping(BaseURI.ALL)
    public ResponseEntity<List<ResUnit>> getAvailableUnits(@RequestBody ReqGetUnits reqGetUnits) {
        return ResponseEntity.ok(lessonService.getAvailableUnits(reqGetUnits));
    }


}