package uz.tune.mentourBiz.rest.service.exercise.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.exception.UnauthorizedAccessException;
import uz.tune.mentourBiz.rest.domain.*;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.SchoolBook;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.postExercise.UnitProgress;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.Course;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.CourseLesson;
import uz.tune.mentourBiz.rest.domain.schoolManagement.schedule.GroupSchedule;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Enrollment;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Level;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.*;
import uz.tune.mentourBiz.rest.payload.req.ReqGetUnits;
import uz.tune.mentourBiz.rest.payload.res.ResLastAttendance;
import uz.tune.mentourBiz.rest.payload.res.ResStudentDashboardSummary;
import uz.tune.mentourBiz.rest.payload.res.lesson.ResActiveHomework;
import uz.tune.mentourBiz.rest.payload.res.lesson.ResGroupActiveHomework;
import uz.tune.mentourBiz.rest.payload.res.lesson.ResUnit;
import uz.tune.mentourBiz.rest.payload.res.school.group.ResGroup;
import uz.tune.mentourBiz.rest.payload.res.user.ResStudentCount;
import uz.tune.mentourBiz.rest.payload.res.user.ResStudentList;
import uz.tune.mentourBiz.rest.payload.studentRes.res.lesson.ResLesson;
import uz.tune.mentourBiz.rest.payload.studentRes.res.lesson.ResLessonSection;
import uz.tune.mentourBiz.rest.payload.studentRes.res.lesson.ResLessonUnits;
import uz.tune.mentourBiz.rest.repository.*;
import uz.tune.mentourBiz.rest.repository.course.CourseRepo;
import uz.tune.mentourBiz.rest.repository.group.GroupRepository;
import uz.tune.mentourBiz.rest.repository.group.LevelRepository;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentRepository;
import uz.tune.mentourBiz.rest.repository.schedule.AttendanceRecordRepository;
import uz.tune.mentourBiz.rest.repository.schedule.GroupScheduleRepository;
import uz.tune.mentourBiz.rest.repository.school.SchoolBookRepository;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.repository.unit.UnitRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.UnitProgressRepository;
import uz.tune.mentourBiz.rest.service.exercise.StudentLessonService;
import uz.tune.mentourBiz.rest.service.exercise.handler.SectionHandler;
import uz.tune.mentourBiz.rest.service.group.enrollment.StudentEnrollmentHelper;
import uz.tune.mentourBiz.rest.service.helper.AuthToViewEntity;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;
import uz.tune.mentourBiz.rest.service.util.MessageSingleton;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentLessonServiceImpl implements StudentLessonService {

    private final UserService userService;
    private final EnrollmentRepository enrollmentRepository;
    private final GroupScheduleRepository scheduleRepository;
    private final UnitProgressRepository unitProgressRepository;
    private final UnitRepository unitRepository;
    private final StudentRepo studentRepo;
    private final CourseRepo courseRepo;
    private final SchoolBookRepository schoolBookRepository;
    private final MessageSingleton messageSingleton;
    private final List<SectionHandler> sectionHandlers;
    private final AuthToViewEntity authToViewEntity;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final UnitExamSessionRepository unitExamSessionRepository;
    private final SchoolExamSettingsRepo schoolExamSettingsRepo;
    private final SchoolSubscriptionRepo schoolSubscriptionRepo;
    private final SchoolAcademicConfigRepo schoolAcademicConfigRepo;
    private final SchoolDirectorRepo schoolDirectorRepo;
    private final UserScopeService userScopeService;
    private final LevelRepository levelRepository;
    private final GroupRepository groupRepository;
    private final uz.tune.mentourBiz.rest.repository.school.SchoolRepo schoolRepo;
    private final StudentParentContactRepository studentParentContactRepository;
    private final StudentEnrollmentHelper studentEnrollmentHelper;

    @Override
    @Transactional(readOnly = true)
    public ResActiveHomework getOneUnit(UUID unitUuid) {
        User currentUser = userService.getCurrentUser();
        Student student = studentRepo.findByUser(currentUser)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));
        Unit unit = unitRepository.findByUuid(unitUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.UNIT_NOT_FOUND.getKey()));
        if (unit.getStatus() == UnitStatus.INACTIVE) {
            throw new EntityNotFoundException(MessageKey.UNIT_NOT_FOUND.getKey());
        }

        // Multi-group: pick the ongoing enrollment whose group actually schedules this unit.
        Enrollment activeEnrollment = studentEnrollmentHelper.resolveEnrollmentForUnit(student, unitUuid);

        GroupSchedule schedule = scheduleRepository.findByGroupUuidAndLessonContainingUnit(
                        activeEnrollment.getGroup().getUuid(),
                        unitUuid,
                        GroupScheduleStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHEDULE_NOT_FOUND.getKey()));

        if (unit.getUnitType().equals(UnitType.EXAM)) {
            Instant now = Instant.now();
            CourseLesson lesson = schedule.getLesson();
            if (now.isBefore(lesson.getStartTime())) {
                throw new PermissionForbidden(MessageKey.EXAM_NOT_STARTED.getKey());
            }
            if (now.isAfter(lesson.getEndTime())) {
                throw new PermissionForbidden(MessageKey.EXAM_CLOSED.getKey());
            }
        } else {
            List<ResLesson> allUnits = getUnitsForCurrentUser(activeEnrollment.getGroup().getUuid());
            ResLesson targetUnit = allUnits.stream()
                    .filter(u -> u.getUnitUuid().equals(unitUuid))
                    .findFirst()
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.UNIT_NOT_SCHEDULED.getKey()));

            if (targetUnit.getOverallStatus() == OverallStatus.LOCKED) {
                throw new PermissionForbidden(MessageKey.UNIT_LOCKED.getKey());
            }
        }

        return buildUnitSummary(student, unit, schedule);
    }

    @Override
    public List<ResLesson> getUnitsForCurrentUserV2() {
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResGroup> getMyGroups() {
        User currentUser = userService.getCurrentUser();
        Student student = studentRepo.findByUser(currentUser)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));
        return studentEnrollmentHelper.getOngoingEnrollments(student.getUser().getUuid())
                .stream()
                .map(e -> new ResGroup(e.getGroup()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ResActiveHomework getActiveHomework(UUID groupUuid) {
        User currentUser = userService.getCurrentUser();
        Student student = studentRepo.findByUser(currentUser)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

        // Multi-group: optional groupUuid selects which group; defaults to the primary group.
        Enrollment enrollment = studentEnrollmentHelper.resolveActiveEnrollment(student, groupUuid);

        List<ResLesson> list = getUnitsForCurrentUser(enrollment.getGroup().getUuid());
        ResLesson activeRes = list.stream()
                .filter(l -> l.getOverallStatus() != OverallStatus.LOCKED && l.getOverallStatus() != OverallStatus.PASSED)
                .findFirst().orElse(null);

        if (activeRes == null) return null;

        Unit unit = unitRepository.findByUuid(activeRes.getUnitUuid()).get();
        GroupSchedule gs = scheduleRepository.findByGroupUuidAndLessonContainingUnit(enrollment.getGroup().getUuid(), unit.getUuid(), GroupScheduleStatus.ACTIVE).get();

        return buildUnitSummary(student, unit, gs);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResGroupActiveHomework> getActiveHomeworkV2(UUID groupUuid) {
        User currentUser = userService.getCurrentUser();
        Student student = studentRepo.findByUser(currentUser)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

        // Multi-group: no groupUuid → every ongoing group; groupUuid given → only that group (403 if not enrolled).
        List<Enrollment> enrollments = (groupUuid != null)
                ? List.of(studentEnrollmentHelper.resolveActiveEnrollment(student, groupUuid))
                : studentEnrollmentHelper.getOngoingEnrollments(student.getUser().getUuid());

        return enrollments.stream()
                .map(enrollment -> {
                    Group group = enrollment.getGroup();
                    ResGroupActiveHomework res = new ResGroupActiveHomework();
                    res.setGroupUuid(group.getUuid());
                    res.setGroupName(group.getName());
                    res.setActiveHomeworks(collectActiveHomeworks(student, group));
                    return res;
                })
                .sorted(Comparator.comparing(ResGroupActiveHomework::getGroupName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .collect(Collectors.toList());
    }

    private List<ResActiveHomework> collectActiveHomeworks(Student student, Group group) {
        List<ResLesson> units = getUnitsForCurrentUser(group.getUuid());
        List<ResActiveHomework> homeworks = new ArrayList<>();
        for (ResLesson lesson : units) {
            if (lesson.getOverallStatus() == OverallStatus.LOCKED || lesson.getOverallStatus() == OverallStatus.PASSED) {
                continue;
            }
            Optional<Unit> unit = unitRepository.findByUuid(lesson.getUnitUuid());
            if (unit.isEmpty()) continue;

            Optional<GroupSchedule> schedule = scheduleRepository.findByGroupUuidAndLessonContainingUnit(
                    group.getUuid(), unit.get().getUuid(), GroupScheduleStatus.ACTIVE);
            if (schedule.isEmpty()) continue;

            homeworks.add(buildUnitSummary(student, unit.get(), schedule.get()));
        }
        return homeworks;
    }

    @Override
    @Transactional(readOnly = true)
    public ResActiveHomework getUnitSummaryForStudent(UUID studentUuid, UUID unitUuid) {
        Student student = studentRepo.findByUserUuid(studentUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

        authToViewEntity.authorizeActionUponStudent(student);

        Unit unit = unitRepository.findByUuid(unitUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.UNIT_NOT_FOUND.getKey()));

        if (unit.getStatus().equals(UnitStatus.INACTIVE)) {
            throw new EntityNotFoundException(MessageKey.UNIT_NOT_FOUND.getKey());
        }

        // Multi-group: pick the ongoing enrollment whose group actually schedules this unit.
        Enrollment enrollment = studentEnrollmentHelper.resolveEnrollmentForUnit(student, unitUuid);

        GroupSchedule schedule = scheduleRepository.findByGroupUuidAndLessonContainingUnit(
                        enrollment.getGroup().getUuid(),
                        unitUuid,
                        GroupScheduleStatus.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.UNIT_NOT_SCHEDULED.getKey()));

        return buildUnitSummary(student, unit, schedule);
    }

    @Override
    @Transactional(readOnly = true)
    public ResLastAttendance getLastAttendanceStatus() {
        User currentUser = userService.getCurrentUser();

        if (!currentUser.getRole().equals(UserRole.STUDENT)) {
            return null;
        }
        Student student=studentRepo.findByUser(currentUser).get();
        School school = student.getSchool();


        return attendanceRecordRepository.findTopByStudentUserUuidOrderByLessonEndTimeDesc(
                        currentUser.getUuid(), Instant.now().plusSeconds(3600L*school.getUtcOffset()))
                .map(record -> new ResLastAttendance(
                        record.getLesson().getName(),
                        record.getLesson().getStartTime().plusSeconds(3600L*school.getUtcOffset()),
                        record.getStatus(),
                        record.getIsMarked()
                ))
                .orElse(null);
    }

    private ResActiveHomework buildUnitSummary(Student student, Unit unit, GroupSchedule schedule) {
        ResActiveHomework response = new ResActiveHomework();
        response.setUnitTitle(unit.getTitle());
        response.setUnitUuid(unit.getUuid());
        response.setDueDate(schedule.getDueDate());
        response.setSortOrder(unit.getSortOrder());

        UUID schoolUuid = student.getSchool().getUuid();
        SchoolSubscription sub = schoolSubscriptionRepo.findBySchool_Uuid(schoolUuid).orElse(null);
        SchoolAcademicConfig academicConfig = schoolAcademicConfigRepo.findBySchool_Uuid(schoolUuid).orElse(new SchoolAcademicConfig());

        response.setAiExplanationEnabled(sub != null && sub.isAiExerciseEnabled());
        if (academicConfig != null) {
            response.setMinScoreToPass(academicConfig.getMinScoreToPass());
        }

        boolean isExam = unit.getUnitType().equals(UnitType.EXAM);
        Instant now = Instant.now();
        CourseLesson lesson = schedule.getLesson();

        List<ResLessonSection> sections = new ArrayList<>();
        for (SectionHandler handler : sectionHandlers) {
            ResLessonSection section = handler.getSectionProgress(student, unit);

            if (section != null) {
                if (isExam) {
                    boolean inWindow = now.isAfter(lesson.getStartTime()) && now.isBefore(lesson.getEndTime());
                    section.setLocked(!inWindow);
                    long secondsLeft = java.time.Duration.between(now, lesson.getEndTime()).getSeconds();
                    section.setRemainingSeconds((int) Math.max(0, secondsLeft));
                } else {
                    section.setLocked(false);
                    section.setRemainingSeconds(null);
                }
                sections.add(section);
            }
        }

        List<LessonSectionType> sortOrder = List.of(
                LessonSectionType.VOCABULARY, LessonSectionType.GRAMMAR, LessonSectionType.LISTENING,
                LessonSectionType.READING, LessonSectionType.WRITING, LessonSectionType.SPEAKING
        );
        sections.sort(Comparator.comparingInt(s -> sortOrder.indexOf(s.getType())));

        response.setSections(sections);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ResStudentDashboardSummary getStudentSummary(UUID schoolUuid) {
        Instant rollingWindowStart = Instant.now().minus(30, ChronoUnit.DAYS);

        // Teachers only see a summary of their own students (enrolled in their groups).
        User currentUser = userService.getCurrentUser();
        if (currentUser.getRole() == UserRole.TEACHER) {
            UUID teacherUuid = currentUser.getUuid();
            long teacherTotal = studentRepo.countActiveStudentsByTeacher(teacherUuid);
            long teacherNew = studentRepo.countNewActiveStudentsByTeacher(teacherUuid, rollingWindowStart, Instant.now());
            long teacherLeft = studentRepo.countLeftThisMonthByTeacher(teacherUuid, rollingWindowStart);
            long teacherWithParents = studentRepo.countStudentsWithParentsByTeacher(teacherUuid);
            double teacherParentRate = (teacherTotal > 0) ? (double) teacherWithParents / teacherTotal * 100 : 0;

            return ResStudentDashboardSummary.builder()
                    .totalStudents(teacherTotal)
                    .activeStudents(teacherTotal)   // a teacher's students are all actively enrolled
                    .inactiveStudents(0L)           // "unassigned" does not apply to a teacher
                    .newThisMonth(teacherNew)
                    .leftThisMonth(teacherLeft)
                    .parentConnectionRate(Math.round(teacherParentRate * 10.0) / 10.0)
                    .build();
        }

        Collection<UUID> schoolUuids = (schoolUuid != null) ?
                List.of(userScopeService.resolveSchoolUuid(schoolUuid)) :
                userScopeService.getAuthorizedSchoolUuids();

        // SYS_ADMIN gets null (= every school); resolve it to the full set so the
        // IN-clause count queries work and we never hit new ArrayList<>(null).
        if (schoolUuids == null) {
            schoolUuids = schoolRepo.findAllActiveSchoolUuids();
        }

        long total = studentRepo.countActiveStudentsIn(new ArrayList<>(schoolUuids));
        long active = enrollmentRepository.countActiveStudentsMulti(schoolUuids);
        long newStudents = studentRepo.countNewActiveStudentsMulti(schoolUuids, rollingWindowStart, Instant.now());
        long left = studentRepo.countLeftThisMonth(schoolUuids, rollingWindowStart);
        long unassigned = studentRepo.countUnassignedMulti(schoolUuids);
        long withParents = studentRepo.countStudentsWithParentsIn(schoolUuids);

        double parentRate = (total > 0) ? (double) withParents / total * 100 : 0;

        return ResStudentDashboardSummary.builder()
                .totalStudents(total)
                .activeStudents(active)
                .inactiveStudents(unassigned)
                .newThisMonth(newStudents)
                .leftThisMonth(left)
                .parentConnectionRate(Math.round(parentRate * 10.0) / 10.0)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResLesson> getUnitsForCurrentUser(UUID requestedGroupUuid) {
        List<LessonUnit> lessonUnits = buildLessonUnitsForCurrentUser(requestedGroupUuid);
        if (lessonUnits == null) return null;
        return lessonUnits.stream().map(LessonUnit::unit).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResLessonUnits> getUnitsGroupedByLesson(UUID requestedGroupUuid) {
        List<LessonUnit> lessonUnits = buildLessonUnitsForCurrentUser(requestedGroupUuid);
        if (lessonUnits == null) return null;

        Map<UUID, ResLessonUnits> grouped = new LinkedHashMap<>();
        for (LessonUnit item : lessonUnits) {
            CourseLesson lesson = item.lesson();
            ResLessonUnits dto = grouped.computeIfAbsent(lesson.getUuid(), key -> {
                ResLessonUnits res = new ResLessonUnits();
                res.setLessonUuid(lesson.getUuid());
                res.setLessonName(lesson.getName());
                res.setLessonDate(lesson.getStartTime().plusSeconds(3600L*lesson.getCourse().getSchool().getUtcOffset()));
                res.setEndTime(lesson.getEndTime().plusSeconds(3600L*lesson.getCourse().getSchool().getUtcOffset()).toString());
                res.setStartTime(lesson.getStartTime().plusSeconds(3600L*lesson.getCourse().getSchool().getUtcOffset()).toString());
                return res;
            });
            dto.getUnits().add(item.unit());
        }
        return new ArrayList<>(grouped.values());
    }

    private record LessonUnit(CourseLesson lesson, ResLesson unit) {
    }

    private List<LessonUnit> buildLessonUnitsForCurrentUser(UUID requestedGroupUuid) {
        User currentUser = userService.getCurrentUser();
        Student student ;
        if (UserRole.PARENT.equals(currentUser.getRole())) {
            List<StudentParentContact> allByPhoneNumberAndIsActiveTrue = studentParentContactRepository.findAllByPhoneNumberAndIsActiveTrue(currentUser.getUsername());
            if (allByPhoneNumberAndIsActiveTrue.isEmpty()) return null;
            if (allByPhoneNumberAndIsActiveTrue.get(0) == null) return null;
            student = allByPhoneNumberAndIsActiveTrue.get(0).getStudent();

        }else {
            student = studentRepo.findByUser(currentUser)
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));
        }
        Enrollment activeEnrollment = studentEnrollmentHelper.resolveActiveEnrollment(student, requestedGroupUuid);

        UUID groupUuid = activeEnrollment.getGroup().getUuid();

        SchoolAcademicConfig academicConfig = schoolAcademicConfigRepo.findBySchool_Uuid(student.getSchool().getUuid())
                .orElse(new SchoolAcademicConfig());
        int minPass = (academicConfig.getMinScoreToPass() != null && academicConfig.getMinScoreToPass() >= 0)
                ? academicConfig.getMinScoreToPass() : 70;

        SchoolExamSettings settings = schoolExamSettingsRepo.findBySchool_Uuid(student.getSchool().getUuid()).orElse(null);

        List<GroupSchedule> schedules = scheduleRepository.findAllByGroup_Uuid(groupUuid)
                .stream()
                .filter(gs -> gs.getStatus() == GroupScheduleStatus.ACTIVE)
                .toList();

        record UnitWithSchedule(Unit unit, GroupSchedule schedule) {
        }
        List<UnitWithSchedule> flattenedUnits = new ArrayList<>();

        for (GroupSchedule gs : schedules) {
            if (gs.getLesson() != null && gs.getLesson().getUnits() != null) {
                for (Unit u : gs.getLesson().getUnits()) {
                    if (u.getStatus() == UnitStatus.ACTIVE) {
                        flattenedUnits.add(new UnitWithSchedule(u, gs));
                    }
                }
            }
        }

        flattenedUnits.sort(Comparator
                .comparing((UnitWithSchedule u) -> u.schedule().getLesson().getStartTime())
                .thenComparing(u -> u.unit().getSortOrder())
        );

        Set<UUID> unitUuids = flattenedUnits.stream().map(u -> u.unit().getUuid()).collect(Collectors.toSet());
        Map<UUID, UnitProgress> progressMap = unitProgressRepository
                .findAllByUnit_UuidInAndStudent_User_Uuid(unitUuids, currentUser.getUuid())
                .stream().collect(Collectors.toMap(p -> p.getUnit().getUuid(), Function.identity()));

        Map<UUID, UnitExamSession> sessionMap = unitExamSessionRepository.findAllByStudentAndUnitUuidIn(student, unitUuids)
                .stream().collect(Collectors.toMap(s -> s.getUnit().getUuid(), s -> s));

        List<LessonUnit> response = new ArrayList<>();
        boolean isPreviousRegularUnitPassed = true;
        Instant now = Instant.now();

        for (UnitWithSchedule item : flattenedUnits) {
            Unit unit = item.unit();
            GroupSchedule gs = item.schedule();
            CourseLesson lesson = gs.getLesson();
            UnitProgress progress = progressMap.get(unit.getUuid());
            UnitExamSession session = sessionMap.get(unit.getUuid());

            boolean isExam = UnitType.EXAM.equals(unit.getUnitType());
            boolean isAdditional = unit.getSchoolBook() != null && !unit.getSchoolBook().isGlobal();
            int currentPercentage = (progress != null) ? progress.getProgressPercentage() : 0;
            boolean isManuallyUnlocked = (progress != null) && progress.isManuallyUnlocked();

            ResLesson dto = new ResLesson();
            dto.setUnitUuid(unit.getUuid());
            dto.setUnitTitle(unit.getTitle());
            dto.setUnitSortOrder(unit.getSortOrder());
            dto.setTopicName(unit.getTopic());
            dto.setDueDate(gs.getDueDate());
            dto.setProgressPercentage(currentPercentage);
            dto.setAdditional(isAdditional);
            dto.setUnitType(unit.getUnitType());

            if (isExam) {
                // Exam Logic...
                if (settings != null) {
                    Map<String, Object> policy = new HashMap<>();
                    policy.put("noScreenshot", settings.isNoScreenshot());
                    policy.put("freezeScreen", settings.isFreezeScreen());
                    policy.put("freezeTimer", settings.getFreezeTimer());

                    long remaining = java.time.Duration.between(now, lesson.getEndTime()).getSeconds();
                    policy.put("globalRemainingSeconds", Math.max(0, remaining));
                    policy.put("isStarted", (session != null) && (now.isAfter(lesson.getStartTime()) && now.isBefore(lesson.getEndTime())));
                    policy.put("isFinished", now.isAfter(lesson.getEndTime()));
                    dto.setExamPolicy(policy);
                }
                if (now.isBefore(lesson.getStartTime())) {
                    dto.setOverallStatus(OverallStatus.LOCKED);
                } else if (now.isAfter(lesson.getEndTime())) {
                    dto.setOverallStatus((session != null && session.isFinished()) ? OverallStatus.PASSED : OverallStatus.LOCKED);
                } else {
                    dto.setOverallStatus(OverallStatus.UNLOCKED);
                }
            } else {
                // FIX 2: Strict Sequential Logic
                boolean meetsPassScore = currentPercentage >= minPass;
                OverallStatus perfStatus = meetsPassScore ? OverallStatus.PASSED : OverallStatus.UNLOCKED;
                boolean isTimeLocked = gs.getDueDate() != null && now.isBefore(gs.getDueDate());

                if (isManuallyUnlocked) {
                    // If manually unlocked (e.g. joined group late), allow entry but only mark PASSED if score is met
                    dto.setOverallStatus(perfStatus);
                } else if (isAdditional) {
                    // Supplementary: Only time-locked
                    dto.setOverallStatus(isTimeLocked ? OverallStatus.LOCKED : perfStatus);
                } else {
                    // Main Curriculum: Must pass time check AND previous unit must be passed
                    if (!isTimeLocked && isPreviousRegularUnitPassed) {
                        dto.setOverallStatus(perfStatus);
                    } else {
                        dto.setOverallStatus(OverallStatus.LOCKED);
                    }
                    // Update the "Gate" for the next unit
                    isPreviousRegularUnitPassed = meetsPassScore;
                }
            }
            response.add(new LessonUnit(lesson, dto));
        }
        return response;
    }

    @Override
    @Transactional
    public List<ResUnit> getAvailableUnits(ReqGetUnits reqGetUnits) {
        List<Unit> units = unitRepository.findAllBySchoolBookUuid(reqGetUnits.getBookUuid());

        User currentUser = userService.getCurrentUser();
        if (!currentUser.getRole().equals(UserRole.SYS_ADMIN)) {
            units = units.stream()
                    .filter(u -> u.getStatus() == UnitStatus.ACTIVE)
                    .toList();
        }

        return units.stream()
                .sorted(Comparator.comparing(Unit::getSortOrder))
                .map(ResUnit::new)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResUnit> getUnitsForGroupByCourse(UUID courseUuid, UUID bookUuid) {
        Course course = courseRepo.findByUuid(courseUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.COURSE_NOT_FOUND.getKey()));

        Set<UUID> assignedUnitUuids = course.getLessons().stream()
                .filter(lesson -> lesson.getStatus() != LessonStatus.DELETED)
                .flatMap(lesson -> lesson.getUnits().stream())
                .map(Unit::getUuid)
                .collect(Collectors.toSet());

        SchoolBook schoolBook = schoolBookRepository.findByUuid(bookUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_BOOK_NOT_FOUND.getKey()));

        List<Unit> allUnitsInBook = unitRepository.findAllBySchoolBookUuidAndStatus(schoolBook.getUuid(), UnitStatus.ACTIVE);

        return allUnitsInBook.stream()
                .filter(unit -> !assignedUnitUuids.contains(unit.getUuid()))
                .sorted(Comparator.comparing(Unit::getSortOrder))
                .map(ResUnit::new)
                .collect(Collectors.toList());
    }

    private GroupSchedule findActiveSchedule(Student student, UUID groupUuid) {
        List<GroupSchedule> schedules = scheduleRepository.findAllByGroup_UuidAndStatusOrderByUnitSortOrderAscDueDateAsc(
                groupUuid, GroupScheduleStatus.ACTIVE);

        if (schedules.isEmpty()) return null;

        SchoolAcademicConfig config = schoolAcademicConfigRepo.findBySchool_Uuid(student.getSchool().getUuid())
                .orElse(new SchoolAcademicConfig());

        record UnitWithSchedule(Unit unit, GroupSchedule schedule) {
        }
        List<UnitWithSchedule> flattened = new ArrayList<>();
        for (GroupSchedule gs : schedules) {
            if (gs.getLesson() != null) {
                for (Unit u : gs.getLesson().getUnits()) {
                    flattened.add(new UnitWithSchedule(u, gs));
                }
            }
        }
        flattened.sort(Comparator.comparing(u -> u.unit().getSortOrder()));

        Set<UUID> unitUuids = flattened.stream().map(u -> u.unit().getUuid()).collect(Collectors.toSet());
        Map<UUID, UnitProgress> progressMap = unitProgressRepository
                .findAllByUnit_UuidInAndStudent_User_Uuid(unitUuids, student.getUser().getUuid())
                .stream().collect(Collectors.toMap(p -> p.getUnit().getUuid(), p -> p));

        Instant now = Instant.now();
        boolean isPreviousRegularPassed = true;
        GroupSchedule backupAdditionalActive = null;

        for (UnitWithSchedule item : flattened) {
            Unit u = item.unit();
            UnitProgress p = progressMap.get(u.getUuid());

            boolean isAdditional = (u.getSchoolBook() != null && !u.getSchoolBook().isGlobal());
            boolean isPassed = (p != null && p.getProgressPercentage() >= config.getMinScoreToPass());
            boolean isManuallyUnlocked = (p != null && p.isManuallyUnlocked());
            boolean isTimeLocked = item.schedule().getDueDate() != null && now.isBefore(item.schedule().getDueDate());

            if (isAdditional) {
                if (!isPassed && (!isTimeLocked || isManuallyUnlocked) && backupAdditionalActive == null) {
                    GroupSchedule gs = item.schedule();
                    gs.setUnit(u);
                    backupAdditionalActive = gs;
                }
            } else {
                if (!isPassed) {
                    if (isPreviousRegularPassed && (!isTimeLocked || isManuallyUnlocked)) {
                        GroupSchedule gs = item.schedule();
                        gs.setUnit(u);
                        return gs;
                    }
                    isPreviousRegularPassed = false;
                } else {
                    isPreviousRegularPassed = true;
                }
            }
        }

        return backupAdditionalActive;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResStudentList> getStudentSummaryDetails(UUID schoolUuid, StudentSummaryMetric metric, Pageable pageable) {
        Collection<UUID> schoolUuids = (schoolUuid != null) ?
                List.of(userScopeService.resolveSchoolUuid(schoolUuid)) :
                userScopeService.getAuthorizedSchoolUuids();

        Instant thirtyDaysAgo = Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS);

        Page<Student> studentPage;

        studentPage = switch (metric) {
            case TOTAL -> studentRepo.findAllBySchool_UuidInAndUser_Status(schoolUuids, UserStatus.ACTIVE, pageable);
            case ACTIVE -> studentRepo.findActiveMulti(schoolUuids, pageable);
            case INACTIVE -> studentRepo.findUnassignedMulti(schoolUuids, pageable);
            case NEW -> studentRepo.findAllBySchool_UuidInAndCreatedAtAfter(schoolUuids, thirtyDaysAgo, pageable);
            case LEFT -> studentRepo.findLeftMulti(schoolUuids, thirtyDaysAgo, pageable);
            case PARENTS -> studentRepo.findWithParentsMulti(schoolUuids, pageable);
        };

        return studentPage.map(ResStudentList::new);
    }

    @Override
    public List<ResStudentCount> getStudentsCount(UUID schoolUuid) {
        if (schoolUuid == null) {
            schoolUuid=userScopeService.getCurrentUserSchoolUuid();
        }
        List<ResStudentCount> resStudentCounts = new ArrayList<>();
        for (Level level : levelRepository.findAll()) {
            ResStudentCount resStudentCount = new ResStudentCount();
            resStudentCount.setCount(0L);
            resStudentCount.setLevel(level.getName());
            List<Group> groupStream = new ArrayList<>();
            if (UserRole.SYS_ADMIN.equals(userService.getCurrentUser().getRole())&&schoolUuid == null) {
                groupStream = groupRepository.findAll().stream().filter(g -> g.getLevel().equals(level)).toList();
            }else {
                groupStream = groupRepository.findAllByBranch_School_Uuid(schoolUuid).stream()
                        .filter(g -> g.getLevel().equals(level)).toList();
            }
            for (Group group : groupStream) {
                resStudentCount.setCount(resStudentCount.getCount() + group.getStudentCount());

            }
            if (resStudentCount.getCount() > 0) {
                resStudentCounts.add(resStudentCount);
            }
        }

        return resStudentCounts;
    }
}