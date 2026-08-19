package uz.tune.mentourBiz.rest.endpoint;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.payload.ResLessonMemberDetail;
import uz.tune.mentourBiz.rest.payload.ResOverviewLesson;
import uz.tune.mentourBiz.rest.service.OverviewServiceImpl;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + "/overview")
@RequiredArgsConstructor
public class OverviewEndpoint {

    private final OverviewServiceImpl overviewService;

    @GetMapping("/schedule")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).SCHEDULE_GET)")
    public ResponseEntity<List<ResOverviewLesson>> getSchedule(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,
            @RequestParam(required = false) UUID schoolUuid) {

        return ResponseEntity.ok(overviewService.getWeeklyLessons(start, end, schoolUuid));
    }

    @GetMapping("/lessons/{lessonUuid}/members")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).LESSON_MEMBERS_GET)")
    public ResponseEntity<List<ResLessonMemberDetail>> getLessonMembers(@PathVariable UUID lessonUuid) {
        return ResponseEntity.ok(overviewService.getLessonMemberDetails(lessonUuid));
    }
}