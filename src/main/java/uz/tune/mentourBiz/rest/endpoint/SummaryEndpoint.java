package uz.tune.mentourBiz.rest.endpoint;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.payload.res.lesson.ResActiveHomework;
import uz.tune.mentourBiz.rest.service.exercise.StudentLessonService;

import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + BaseURI.SUMMARY)
@RequiredArgsConstructor
public class SummaryEndpoint {

    private final StudentLessonService studentLessonService;

    @GetMapping("/student/{studentUuid}/unit/{unitUuid}/summary")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).STUDENT_HOMEWORK_SUMMARY_GET)")
    public ResponseEntity<ResActiveHomework> getStudentHomeworkSummary(
            @PathVariable UUID studentUuid,
            @PathVariable UUID unitUuid) {
        return ResponseEntity.ok(studentLessonService.getUnitSummaryForStudent(studentUuid, unitUuid));
    }
}
