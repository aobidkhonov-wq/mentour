package uz.tune.mentourBiz.rest.endpoint.studentApp;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.payload.res.lesson.ResGroupActiveHomework;
import uz.tune.mentourBiz.rest.service.exercise.StudentLessonService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API2 + BaseURI.UNITS)
@RequiredArgsConstructor
public class StudentLessonEndpointV2 {

    private final StudentLessonService lessonService;

    @GetMapping("/active-homework")
    public ResponseEntity<List<ResGroupActiveHomework>> getActiveHomework(@RequestParam(name = "groupUuid", required = false) UUID groupUuid) {
        return ResponseEntity.ok(lessonService.getActiveHomeworkV2(groupUuid));
    }
}
