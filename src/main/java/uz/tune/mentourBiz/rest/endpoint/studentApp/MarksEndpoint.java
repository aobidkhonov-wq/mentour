package uz.tune.mentourBiz.rest.endpoint.studentApp;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.payload.studentRes.res.lesson.ResMarksDashboard;
import uz.tune.mentourBiz.rest.service.group.schedule.MarksService;

import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + "/marks")
@RequiredArgsConstructor
public class MarksEndpoint {

    private final MarksService marksService;

    @GetMapping("/course/{courseUuid}")
    public ResponseEntity<ResMarksDashboard> getMarksForGroup(
            @PathVariable UUID courseUuid,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ResponseEntity.ok(marksService.getMarksForGroup(courseUuid, year, month));
    }
}