package uz.tune.mentourBiz.rest.service.school.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.SchoolAcademicConfig;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.ExerciseTask;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.postExercise.TaskAttempt;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.MessageKey;
import uz.tune.mentourBiz.rest.payload.req.attempt.ReqResetAttempt;
import uz.tune.mentourBiz.rest.payload.req.attempt.ReqSetAttemptLimit;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.attempt.ResAttemptStatus;
import uz.tune.mentourBiz.rest.payload.res.attempt.SchoolLimitAttempts;
import uz.tune.mentourBiz.rest.repository.SchoolAcademicConfigRepo;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseTaskRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.TaskAttemptRepository;
import uz.tune.mentourBiz.rest.service.helper.AuthToViewEntity;
import uz.tune.mentourBiz.rest.service.school.SchoolAttemptService;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SchoolAttemptServiceImpl implements SchoolAttemptService {

    private static final int DEFAULT_LIMIT = 9993;

    private final UserService userService;
    private final UserScopeService userScopeService;
    private final StudentRepo studentRepo;
    private final SchoolRepo schoolRepo;
    private final SchoolAcademicConfigRepo academicConfigRepo;
    private final ExerciseTaskRepository exerciseTaskRepository;
    private final TaskAttemptRepository taskAttemptRepository;
    private final AuthToViewEntity authToViewEntity;

    @Override
    @Transactional(readOnly = true)
    public SchoolLimitAttempts getSchoolAttemptLimit(UUID schoolUuid) {
        UUID resolved = userScopeService.resolveSchoolUuid(schoolUuid);
        if (resolved == null) throw new ValidationException(MessageKey.SCHOOL_CONTEXT_REQUIRED.getKey());
        int schoolLimit = resolveSchoolLimit(resolved);
        if (schoolLimit == DEFAULT_LIMIT) {
            return new SchoolLimitAttempts(false, schoolLimit);
        } else {
            return new SchoolLimitAttempts(true, schoolLimit);
        }
    }

    @Override
    @Transactional
    public ResponseMessage setSchoolAttemptLimit(ReqSetAttemptLimit request) {
        UUID schoolUuid = userScopeService.resolveSchoolUuid(request.getSchoolUuid());
        if (schoolUuid == null) throw new ValidationException(MessageKey.SCHOOL_CONTEXT_REQUIRED.getKey());

        SchoolAcademicConfig config = academicConfigRepo.findBySchool_Uuid(schoolUuid)
                .orElseGet(() -> {
                    School school = schoolRepo.findByUuid(schoolUuid)
                            .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));
                    SchoolAcademicConfig fresh = new SchoolAcademicConfig();
                    fresh.setSchool(school);
                    return fresh;
                });

        config.setHomeworkAttemptLimit(request.getLimit());
        academicConfigRepo.save(config);
        return new ResponseMessage("Homework attempt limit updated to " + request.getLimit());
    }

    @Override
    @Transactional(readOnly = true)
    public ResAttemptStatus getStatus(UUID taskUuid,Student student) {
        ExerciseTask task = findTask(taskUuid);
        int limit = resolveSchoolLimit(student.getSchool().getUuid());
        TaskAttempt attempt = taskAttemptRepository.findByStudent_UuidAndTask_Uuid(student.getUuid(), task.getUuid())
                .orElseGet(() -> newAttempt(student, task));
        return buildStatus(task.getUuid(), limit, attempt);
    }

    @Override
    @Transactional
    public ResAttemptStatus consumeAttempt(UUID taskUuid) {
        Student student = currentStudent();
        ExerciseTask task = findTask(taskUuid);
        int limit = resolveSchoolLimit(student.getSchool().getUuid());

        TaskAttempt attempt = taskAttemptRepository.findByStudent_UuidAndTask_Uuid(student.getUuid(), task.getUuid())
                .orElseGet(() -> newAttempt(student, task));

        int effectiveLimit = limit + attempt.getExtraAttempts();
        if (attempt.getAttemptsUsed() >= effectiveLimit) {
            throw new ValidationException(MessageKey.HOMEWORK_ATTEMPT_LIMIT_REACHED.getKey());
        }

        attempt.setAttemptsUsed(attempt.getAttemptsUsed() + 1);
        taskAttemptRepository.save(attempt);
        return buildStatus(task.getUuid(), limit, attempt);
    }

    @Override
    @Transactional
    public ResAttemptStatus resetAttempt(ReqResetAttempt request) {
        Student student = studentRepo.findByUserUuid(request.getStudentUuid())
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));
        authToViewEntity.authorizeActionUponStudent(student);

        ExerciseTask task = findTask(request.getTaskUuid());

        TaskAttempt attempt = taskAttemptRepository.findByStudent_UuidAndTask_Uuid(student.getUuid(), task.getUuid())
                .orElseGet(() -> newAttempt(student, task));

        if (request.getExtraAttempts() != null && request.getExtraAttempts() > 0) {
            attempt.setExtraAttempts(attempt.getExtraAttempts() + request.getExtraAttempts());
        } else {
            attempt.setAttemptsUsed(0);
        }

        taskAttemptRepository.save(attempt);
        int limit = resolveSchoolLimit(student.getSchool().getUuid());
        return buildStatus(task.getUuid(), limit, attempt);
    }


    private Student currentStudent() {
        User user = userService.getCurrentUser();
        return studentRepo.findByUser(user)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));
    }

    private ExerciseTask findTask(UUID taskUuid) {
        return exerciseTaskRepository.findByUuid(taskUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXERCISE_TASK_NOT_FOUND.getKey()));
    }

    private TaskAttempt newAttempt(Student student, ExerciseTask task) {
        TaskAttempt attempt = new TaskAttempt();
        attempt.setStudent(student);
        attempt.setTask(task);
        return attempt;
    }

    private int resolveSchoolLimit(UUID schoolUuid) {
        return academicConfigRepo.findBySchool_Uuid(schoolUuid)
                .map(c -> c.isAttemptInstalled() ? c.getMaxRetries() : DEFAULT_LIMIT)
                .orElse(DEFAULT_LIMIT);
    }

    private ResAttemptStatus buildStatus(UUID taskUuid, int schoolLimit, TaskAttempt attempt) {
        int effectiveLimit = schoolLimit + attempt.getExtraAttempts();
        int remaining = Math.max(0, effectiveLimit - attempt.getAttemptsUsed());
        return new ResAttemptStatus(
                taskUuid,
                schoolLimit,
                attempt.getExtraAttempts(),
                attempt.getAttemptsUsed(),
                remaining,
                remaining > 0
        );
    }
}
