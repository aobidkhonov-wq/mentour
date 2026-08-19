package uz.tune.mentourBiz.rest.endpoint;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.enums.LessonSectionType;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.repository.unit.UnitRepository;
import uz.tune.mentourBiz.rest.service.ExamService;
import uz.tune.mentourBiz.rest.service.user.UserService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(BaseURI.API1 + "/exam")
@RequiredArgsConstructor
public class StudentExamEndpoint {

    private final ExamService examService;
    private final StudentRepo studentRepo;
    private final UnitRepository unitRepo;
    private final UserService userService;

    @PostMapping("/{unitUuid}/start")
    public ResponseEntity<Map<String, Object>> startExam(@PathVariable UUID unitUuid) {
        Student student = studentRepo.findByUser(userService.getCurrentUser()).orElseThrow();
        Unit unit = unitRepo.findByUuid(unitUuid).orElseThrow();
        return ResponseEntity.ok(examService.initiateExam(student, unit));
    }

    @GetMapping("/{unitUuid}/resume")
    public ResponseEntity<Map<String, Object>> resumeExam(@PathVariable UUID unitUuid) {
        Student student = studentRepo.findByUser(userService.getCurrentUser()).orElseThrow();
        Unit unit = unitRepo.findByUuid(unitUuid).orElseThrow();
        return ResponseEntity.ok(examService.getExamPolicy(student, unit));
    }

    @PostMapping("/{unitUuid}/section/stop")
    public ResponseEntity<ResponseMessage> stopSection(@PathVariable UUID unitUuid) {
        Student student = studentRepo.findByUser(userService.getCurrentUser()).orElseThrow();
        Unit unit = unitRepo.findByUuid(unitUuid).orElseThrow();
        examService.stopSection(student, unit);
        return ResponseEntity.ok(new ResponseMessage("Section stopped."));
    }

    @PostMapping("/{unitUuid}/section/{sectionType}/start")
    public ResponseEntity<ResponseMessage> startSection(@PathVariable UUID unitUuid, @PathVariable LessonSectionType sectionType) {
        Student student = studentRepo.findByUser(userService.getCurrentUser()).orElseThrow();
        Unit unit = unitRepo.findByUuid(unitUuid).orElseThrow();
        examService.startSection(student, unit, sectionType);
        return ResponseEntity.ok(new ResponseMessage("Section started."));
    }
}