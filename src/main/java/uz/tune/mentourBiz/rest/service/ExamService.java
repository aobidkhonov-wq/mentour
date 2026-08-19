package uz.tune.mentourBiz.rest.service;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.SchoolExamSettings;
import uz.tune.mentourBiz.rest.domain.UnitExamSession;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.CourseLesson;
import uz.tune.mentourBiz.rest.domain.schoolManagement.schedule.GroupSchedule;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.enums.EnrollmentStatus;
import uz.tune.mentourBiz.rest.enums.GroupScheduleStatus;
import uz.tune.mentourBiz.rest.enums.LessonSectionType;
import uz.tune.mentourBiz.rest.repository.SchoolExamSettingsRepo;
import uz.tune.mentourBiz.rest.repository.UnitExamSessionRepository;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentRepository;
import uz.tune.mentourBiz.rest.repository.schedule.AttendanceRecordRepository;
import uz.tune.mentourBiz.rest.repository.schedule.GroupScheduleRepository;
import uz.tune.mentourBiz.rest.service.group.enrollment.StudentEnrollmentHelper;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final UnitExamSessionRepository sessionRepo;
    private final AttendanceRecordRepository attendanceRepo;
    private final GroupScheduleRepository scheduleRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final SchoolExamSettingsRepo schoolExamSettingsRepo;
    private final StudentEnrollmentHelper studentEnrollmentHelper;

    public Map<String, Object> getExamPolicy(Student student, Unit unit) {
        // Fetch settings specifically for the school the student belongs to
        SchoolExamSettings settings = schoolExamSettingsRepo.findBySchool_Uuid(student.getSchool().getUuid())
                .orElseGet(() -> {
                    SchoolExamSettings defaults = new SchoolExamSettings();
                    defaults.setSchool(student.getSchool());
                    defaults.setNoScreenshot(true);
                    defaults.setTimeLimit(60);
                    return schoolExamSettingsRepo.save(defaults);
                });

        UUID groupUuid = studentEnrollmentHelper.resolveEnrollmentForUnit(student, unit.getUuid())
                .getGroup().getUuid();

        GroupSchedule schedule = scheduleRepo.findByGroupUuidAndLessonContainingUnit(
                        groupUuid, unit.getUuid(), GroupScheduleStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXAM_SESSION_NOT_FOUND.getKey()));

        CourseLesson lesson = schedule.getLesson();
        Instant now = Instant.now();

        long secondsLeft = 0;
        if (lesson.getEndTime() != null && now.isBefore(lesson.getEndTime())) {
            secondsLeft = Duration.between(now, lesson.getEndTime()).getSeconds();
        }

        UnitExamSession session = sessionRepo.findByStudentAndUnit(student, unit).orElse(null);

        Map<String, Object> policy = new HashMap<>();
        policy.put("noScreenshot", settings.isNoScreenshot());
        policy.put("freezeScreen", settings.isFreezeScreen());
        policy.put("separateSection", settings.isSeparateSection());
        policy.put("timeLimit", settings.getTimeLimit());
        policy.put("globalRemainingSeconds", Math.max(0, secondsLeft));
        policy.put("isStarted", (session != null));
        policy.put("isFinished", session != null && session.isFinished());

        return policy;
    }

    @Transactional
    public Map<String, Object> initiateExam(Student student, Unit unit) {
        if (!sessionRepo.existsByStudentAndUnit(student, unit)) {
            UnitExamSession session = new UnitExamSession();
            session.setStudent(student);
            session.setUnit(unit);
            session.setStartTime(Instant.now());
            sessionRepo.saveAndFlush(session);
        }
        return getExamPolicy(student, unit);
    }

    @Transactional
    public void startSection(Student student, Unit unit, LessonSectionType sectionType) {
        UnitExamSession session = sessionRepo.findByStudentAndUnit(student, unit)
                .orElseThrow(() -> new ValidationException(MessageKey.EXAM_SESSION_NOT_FOUND.getKey()));

        SchoolExamSettings settings = schoolExamSettingsRepo.findBySchool_Uuid(student.getSchool().getUuid())
                .orElseGet(() -> {
                    SchoolExamSettings defaults = new SchoolExamSettings();
                    defaults.setSchool(student.getSchool());
                    defaults.setNoScreenshot(true);
                    defaults.setSeparateSection(false);
                    defaults.setAttemptLimit(1);
                    defaults.setFreezeScreen(true);
                    defaults.setFreezeTimer(120);
                    defaults.setTimeLimit(60);
                    return schoolExamSettingsRepo.save(defaults);
                });

        if (!settings.isSeparateSection()) {
            throw new ValidationException(MessageKey.EXAM_POLICY_TIMER_DISABLED.getKey());
        }

        if (session.getCurrentSection() == sectionType) return;
        if (session.getCurrentSection() != null) stopSection(student, unit);

        Integer minutesLimit = settings.getSectionTimeLimits().get(sectionType);
        if (minutesLimit == null || minutesLimit <= 0) {
            throw new ValidationException(MessageKey.EXAM_TIME_EXPIRED.getKey());
        }

        int alreadyUsed = session.getElapsedSeconds().getOrDefault(sectionType, 0);
        int remaining = (minutesLimit * 60) - alreadyUsed;

        if (remaining <= 0) {
            throw new ValidationException(MessageKey.EXAM_TIME_EXPIRED.getKey());
        }

        session.setCurrentSection(sectionType);
        session.setSectionEndTime(Instant.now().plus(Duration.ofSeconds(remaining)));
        sessionRepo.save(session);
    }

    @Transactional
    public void stopSection(Student student, Unit unit) {
        UnitExamSession session = sessionRepo.findByStudentAndUnit(student, unit)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXAM_SESSION_NOT_FOUND.getKey()));

        if (session.getCurrentSection() == null || session.getSectionEndTime() == null) return;

        SchoolExamSettings settings = schoolExamSettingsRepo.findBySchool_Uuid(student.getSchool().getUuid())
                .orElseGet(() -> {
                    SchoolExamSettings defaults = new SchoolExamSettings();
                    defaults.setSchool(student.getSchool());
                    defaults.setNoScreenshot(true);
                    defaults.setSeparateSection(false);
                    defaults.setAttemptLimit(1);
                    defaults.setFreezeScreen(true);
                    defaults.setFreezeTimer(120);
                    defaults.setTimeLimit(60);
                    return schoolExamSettingsRepo.save(defaults);
                });

        Integer minutesLimit = settings.getSectionTimeLimits().get(session.getCurrentSection());
        int limitSecs = (minutesLimit != null) ? minutesLimit * 60 : 0;

        long remaining = Duration.between(Instant.now(), session.getSectionEndTime()).getSeconds();
        int usedBefore = session.getElapsedSeconds().getOrDefault(session.getCurrentSection(), 0);

        int spentThisTurn = (int) (limitSecs - usedBefore - Math.max(0, remaining));

        session.getElapsedSeconds().put(session.getCurrentSection(), usedBefore + spentThisTurn);
        session.setCurrentSection(null);
        session.setSectionEndTime(null);
        sessionRepo.save(session);
    }

    public void validateAccess(Student student, Unit unit, LessonSectionType sectionType) {
        Instant now = Instant.now();

        UUID groupUuid = studentEnrollmentHelper.resolveEnrollmentForUnit(student, unit.getUuid())
                .getGroup().getUuid();

        GroupSchedule schedule = scheduleRepo.findByGroupUuidAndLessonContainingUnit(
                        groupUuid, unit.getUuid(), GroupScheduleStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.EXAM_SESSION_NOT_FOUND.getKey()));

        CourseLesson lesson = schedule.getLesson();

        if (now.isBefore(lesson.getStartTime()))
            throw new PermissionForbidden(MessageKey.EXAM_NOT_STARTED.getKey());
        if (now.isAfter(lesson.getEndTime()))
            throw new PermissionForbidden(MessageKey.EXAM_CLOSED.getKey());

        UnitExamSession session = sessionRepo.findByStudentAndUnit(student, unit)
                .orElseGet(() -> {
                    UnitExamSession newSession = new UnitExamSession();
                    newSession.setStudent(student);
                    newSession.setUnit(unit);
                    newSession.setStartTime(now);
                    return sessionRepo.save(newSession);
                });

        if (session.isBlocked()) throw new PermissionForbidden(MessageKey.EXAM_BLOCKED.getKey());
        if (session.isFinished()) throw new ValidationException(MessageKey.EXERCISE_PASSED.getKey());
    }
}