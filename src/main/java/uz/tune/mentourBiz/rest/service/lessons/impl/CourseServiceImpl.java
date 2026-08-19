package uz.tune.mentourBiz.rest.service.lessons.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.Organization;
import uz.tune.mentourBiz.rest.domain.PaymentPackage;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.SchoolBook;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.Course;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.CourseLesson;
import uz.tune.mentourBiz.rest.domain.schoolManagement.schedule.AttendanceRecord;
import uz.tune.mentourBiz.rest.domain.schoolManagement.schedule.GroupSchedule;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Enrollment;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Teacher;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.*;
import uz.tune.mentourBiz.rest.payload.ResCourseGroupDetails;
import uz.tune.mentourBiz.rest.payload.req.course.ReqCourse;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;
import uz.tune.mentourBiz.rest.payload.res.course.ResCourseList;
import uz.tune.mentourBiz.rest.payload.res.course.ResCourseView;
import uz.tune.mentourBiz.rest.payload.res.course.ResCreateCourse;
import uz.tune.mentourBiz.rest.repository.PaymentPackageRepo;
import uz.tune.mentourBiz.rest.repository.SchoolDirectorRepo;
import uz.tune.mentourBiz.rest.repository.course.CourseLessonRepo;
import uz.tune.mentourBiz.rest.repository.course.CourseRepo;
import uz.tune.mentourBiz.rest.repository.group.GroupRepository;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentRepository;
import uz.tune.mentourBiz.rest.repository.schedule.AttendanceRecordRepository;
import uz.tune.mentourBiz.rest.repository.schedule.GroupScheduleRepository;
import uz.tune.mentourBiz.rest.repository.school.SchoolBookRepository;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.repository.unit.UnitRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.UnitProgressRepository;
import uz.tune.mentourBiz.rest.repository.user.TeacherRepository;
import uz.tune.mentourBiz.rest.service.exercise.impl.StudentLessonServiceImpl;
import uz.tune.mentourBiz.rest.service.group.schedule.ScheduleService;
import uz.tune.mentourBiz.rest.service.helper.AuthToViewEntity;
import uz.tune.mentourBiz.rest.service.lessons.CourseService;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;
import uz.tune.mentourBiz.rest.service.util.MessageSingleton;
import uz.tune.mentourBiz.utils.CoreUtils;
import uz.tune.mentourBiz.utils.DateUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepo courseRepo;
    private final CourseLessonRepo courseLessonRepo;
    private final SchoolRepo schoolRepo;


    private final GroupRepository groupRepository;
    private final MessageSingleton messageSingleton;
    private final UserService userService;
    private final UserScopeService userScopeService;

    //    private final BigBlueButtonService bigBlueButtonService;
    private final StudentRepo studentRepo;
    private final SchoolBookRepository schoolBookRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final GroupScheduleRepository groupScheduleRepository;
    private final StudentLessonServiceImpl studentLessonServiceImpl;
    private final CourseLessonServiceImpl courseLessonServiceImpl;
    private final TeacherRepository teacherRepository;
    private final ScheduleService scheduleService;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final PaymentPackageRepo paymentPackageRepo;
    private final UnitProgressRepository unitProgressRepository;
    private final UnitRepository unitRepository;
    private final AuthToViewEntity authToViewEntity;
    private final SchoolDirectorRepo schoolDirectorRepo;
    private final uz.tune.mentourBiz.rest.repository.SchoolAcademicConfigRepo schoolAcademicConfigRepo;

    @Override
    @Transactional(readOnly = true)
    public Page<ResCourseList> getAllCourses(Pageable pageable, CourseStatus status, UUID schoolId,
                                             String courseName, String className, String bookName,
                                             UUID packageUuid) {
        User currentUser = userService.getCurrentUser();
        CourseStatus filterStatus = (status != null) ? status : CourseStatus.ACTIVE;

        UUID resolvedId = userScopeService.resolveSchoolUuid(schoolId);

        Collection<UUID> schoolUuids;
        if (resolvedId != null) {
            schoolUuids = List.of(resolvedId);
        } else {
            schoolUuids = userScopeService.getAuthorizedSchoolUuids();
        }

        Long scopeMentorId = (currentUser.getRole() == UserRole.MENTOR) ? currentUser.getId() : null;
        List<UUID> scopeGroupUuids = null;

        if (currentUser.getRole() == UserRole.TEACHER) {
            // Teachers only see courses for groups they teach
            scopeGroupUuids = groupRepository.findAllByTeacher_User_Uuid(currentUser.getUuid())
                    .stream().map(Group::getUuid).toList();
            if (scopeGroupUuids.isEmpty()) return Page.empty(pageable);
        } else if (currentUser.getRole() == UserRole.STUDENT) {
            //  Students only see courses for groups they are actively enrolled in
            scopeGroupUuids = enrollmentRepository.findAllByStudent_User_UuidAndStatus(
                            currentUser.getUuid(), EnrollmentStatus.ONGOING)
                    .stream()
                    .map(e -> e.getGroup().getUuid())
                    .collect(Collectors.toList());

            // If the student has no active enrollments, return an empty page immediately
            if (scopeGroupUuids.isEmpty()) {
                return Page.empty(pageable);
            }
        }
        // 3. Query the database with the restricted group IDs
        Page<Course> coursesPage = courseRepo.findWithFilters(
                filterStatus,
                schoolUuids,
                scopeMentorId,
                scopeGroupUuids, // This limits the SQL query results
                courseName,
                className,
                bookName,
                packageUuid,
                pageable);

        Map<UUID, CourseDurationSetting> durationSettingCache = new HashMap<>();

        return coursesPage.map(course -> {
            long studentCount = (course.getGroup() != null) ?
                    enrollmentRepository.countByGroup_UuidAndStatusAndStudent_User_Status(course.getGroup().getUuid(), EnrollmentStatus.ONGOING, UserStatus.ACTIVE) : 0;

            CourseLesson latestLesson = course.getLessons().stream()
                    .filter(l -> !l.getStatus().equals(LessonStatus.DELETED))
                    .filter(l -> l.getStartTime().isBefore(java.time.Instant.now()))
                    .max(Comparator.comparing(CourseLesson::getStartTime))
                    .orElse(null);

            String lName = (latestLesson != null) ? latestLesson.getName() : "N/A";
            String uName = "N/A";

            if (latestLesson != null && latestLesson.getUnits() != null) {
                uName = latestLesson.getUnits().stream()
                        .filter(u -> u.getStatus().equals(UnitStatus.ACTIVE))
                        .max(Comparator.comparing(Unit::getSortOrder))
                        .map(Unit::getTitle)
                        .orElse("N/A");
            }

            double courseAvgProgress = calculateAverageCourseProgress(course.getUuid());
            ResCourseList dto = new ResCourseList(course, course.getLessonCount(), uName, studentCount, courseAvgProgress);
            dto.setLatestLessonName(lName);
            dto.setLatestUnitName(uName);
            dto.setLastLessonIndex(getLastLessonIndex(course));

            UUID courseSchoolUuid = (course.getSchool() != null) ? course.getSchool().getUuid() : null;
            CourseDurationSetting durationSetting = (courseSchoolUuid != null)
                    ? durationSettingCache.computeIfAbsent(courseSchoolUuid, this::resolveCourseDurationSetting)
                    : new CourseDurationSetting(false, 90);
            CourseDuration cd = computeCourseDuration(course, durationSetting);
            dto.setCourseStartDate(course.getCourseStartDate());
            dto.setCourseDueDate(cd.dueDate());
            dto.setOverdue(cd.overdue());
            dto.setDaysOverdue(cd.daysOverdue());
            dto.setDurationProgressPercent(cd.durationProgressPercent());

            return dto;
        });
    }


    @Override
    @Transactional(readOnly = true)
    public ResCourseView getCourseById(UUID courseId) {
        User currentUser = userService.getCurrentUser();
        Course course = courseRepo.findByUuid(courseId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.COURSE_NOT_FOUND.getKey()));

        authToViewEntity.authorizeUponCourse(currentUser, course);

        double courseAvgProgress = 0.0;
        Map<UUID, AttendanceStatus> attendanceMap = new HashMap<>();
        if (currentUser.getRole().equals(UserRole.STUDENT)) {
            attendanceMap = attendanceRecordRepository.findAllByStudent_User_Uuid(currentUser.getUuid())
                    .stream().collect(Collectors.toMap(r -> r.getLesson().getUuid(), AttendanceRecord::getStatus, (a, b) -> a));
            courseAvgProgress = calculateAverageCourseProgress(course.getUuid());
        }

        long totalMinutes = course.getLessons().stream()
                .filter(l -> !l.getStatus().equals(LessonStatus.DELETED))
                .filter(l -> l.getStartTime() != null && l.getEndTime() != null)
                .mapToLong(l -> java.time.Duration.between(l.getStartTime(), l.getEndTime()).toMinutes())
                .sum();

        long totalHours = totalMinutes / 60;

        int activeStudentCount = (int) enrollmentRepository.countByGroup_UuidAndStatusAndStudent_User_Status(
                course.getGroup().getUuid(), EnrollmentStatus.ONGOING, UserStatus.ACTIVE);

        ResCourseView res = new ResCourseView(
                course,
                totalHours,
                currentUser,
                attendanceMap,
                activeStudentCount,
                courseAvgProgress
        );

        res.setLastLessonIndex(getLastLessonIndex(course));

        CourseDurationSetting durationSetting = resolveCourseDurationSetting(course.getSchool().getUuid());
        CourseDuration cd = computeCourseDuration(course, durationSetting);
        res.setCourseStartDate(course.getCourseStartDate());
        res.setCourseDueDate(cd.dueDate());
        res.setOverdue(cd.overdue());
        res.setDaysOverdue(cd.daysOverdue());
        res.setDurationProgressPercent(cd.durationProgressPercent());

        return res;
    }

    @Override
    @Transactional(readOnly = true)
    public ResCourseView getCourseViewForStudent(UUID courseId, UUID studentUserUuid) {
        Course course = courseRepo.findByUuid(courseId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.COURSE_NOT_FOUND.getKey()));
        Student student = studentRepo.findByUserUuid(studentUserUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

        Map<UUID, AttendanceStatus> attendanceMap = attendanceRecordRepository.findAllByStudent_User_Uuid(studentUserUuid)
                .stream().filter(r -> r.getLesson() != null)
                .collect(Collectors.toMap(r -> r.getLesson().getUuid(), AttendanceRecord::getStatus, (a, b) -> a));

        double courseAvgProgress = calculateCourseProgressForStudent(course.getUuid(), student.getId());

        long totalMinutes = course.getLessons().stream()
                .filter(l -> !l.getStatus().equals(LessonStatus.DELETED))
                .filter(l -> l.getStartTime() != null && l.getEndTime() != null)
                .mapToLong(l -> java.time.Duration.between(l.getStartTime(), l.getEndTime()).toMinutes())
                .sum();
        long totalHours = totalMinutes / 60;

        int activeStudentCount = (int) enrollmentRepository.countByGroup_UuidAndStatusAndStudent_User_Status(
                course.getGroup().getUuid(), EnrollmentStatus.ONGOING, UserStatus.ACTIVE);

        ResCourseView res = new ResCourseView(course, totalHours, student.getUser(), attendanceMap, activeStudentCount, courseAvgProgress);
        res.setLastLessonIndex(getLastLessonIndex(course));

        CourseDurationSetting durationSetting = resolveCourseDurationSetting(course.getSchool().getUuid());
        CourseDuration cd = computeCourseDuration(course, durationSetting);
        res.setCourseStartDate(course.getCourseStartDate());
        res.setCourseDueDate(cd.dueDate());
        res.setOverdue(cd.overdue());
        res.setDaysOverdue(cd.daysOverdue());
        res.setDurationProgressPercent(cd.durationProgressPercent());
        return res;
    }

    // Average course progress for a specific student (parameterised version of calculateAverageCourseProgress).
    private Double calculateCourseProgressForStudent(UUID courseUuid, Long studentId) {
        Course course = courseRepo.findByUuid(courseUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.COURSE_NOT_FOUND.getKey()));

        List<SchoolBook> linkedBooks = course.getLinkedBooks();
        Long schoolBookId = (!CoreUtils.isEmpty(linkedBooks)) ? linkedBooks.get(0).getId() : 0L;

        List<Long> unitIds = (schoolBookId == 0L)
                ? unitRepository.findUnitsByCourseUuid(courseUuid)
                : unitRepository.findAllBySchoolBookIdAndStatus(schoolBookId, UnitStatus.ACTIVE).stream().map(Unit::getId).toList();

        if (unitIds == null || unitIds.isEmpty()) return 0.0;

        Double avg = unitProgressRepository.getAverageProgressForStudentsAndUnitsV2(studentId, unitIds);
        return (avg != null) ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }

    // ---- Course duration soft limit ----
    // A course is "overdue" once it has been running longer than the school's configured
    // course length (defaultCourseLengthDays). It is a soft indicator only - nothing is blocked.

    private record CourseDuration(java.time.Instant dueDate, boolean overdue, int daysOverdue,
                                  Integer durationProgressPercent) {
    }

    // Overdue tracking is driven entirely by the school's academic config: it is only active when
    // defaultCourseLengthInstalled is enabled, and the limit is defaultCourseLengthDays.
    private record CourseDurationSetting(boolean installed, int durationDays) {
    }

    private CourseDurationSetting resolveCourseDurationSetting(UUID schoolUuid) {
        return schoolAcademicConfigRepo.findBySchool_Uuid(schoolUuid)
                .map(c -> new CourseDurationSetting(
                        c.isDefaultCourseLengthInstalled(),
                        (c.getDefaultCourseLengthDays() != null && c.getDefaultCourseLengthDays() > 0)
                                ? c.getDefaultCourseLengthDays() : 90))
                .orElse(new CourseDurationSetting(false, 90));
    }

    private CourseDuration computeCourseDuration(Course course, CourseDurationSetting setting) {
        boolean hasOwnDuration = course.getDuration() != null && course.getDuration() > 0;
        if (!hasOwnDuration && !setting.installed()) return new CourseDuration(null, false, 0, null);

        java.time.Instant start = (course.getCourseStartDate() != null)
                ? course.getCourseStartDate() : course.getCreatedAt();
        if (start == null) return new CourseDuration(null, false, 0, null);

        int durationDays = hasOwnDuration ? course.getDuration() : setting.durationDays();
        java.time.Instant due = start.plus(durationDays, java.time.temporal.ChronoUnit.DAYS);
        java.time.Instant now = java.time.Instant.now();
        boolean overdue = now.isAfter(due);
        int daysOverdue = overdue ? (int) java.time.temporal.ChronoUnit.DAYS.between(due, now) : 0;

        long totalMillis = java.time.Duration.between(start, due).toMillis();
        long elapsedMillis = java.time.Duration.between(start, now).toMillis();
        int durationProgressPercent = (totalMillis <= 0) ? 100
                : (int) Math.min(100, Math.max(0, (elapsedMillis * 100) / totalMillis));

        return new CourseDuration(due, overdue, daysOverdue, durationProgressPercent);
    }

    @Transactional(readOnly = true)
    public ResCourseGroupDetails getCourseGroupDetails(UUID courseUuid) {
        Course course = courseRepo.findByUuid(courseUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.COURSE_NOT_FOUND.getKey()));

        authToViewEntity.authorizeUponCourse(userService.getCurrentUser(), course);

        Group group = course.getGroup();
        Teacher teacher = group.getTeacher();

        List<Enrollment> enrollments = enrollmentRepository.findAllByGroup_UuidAndStatusAndStudent_User_Status(
                group.getUuid(), EnrollmentStatus.ONGOING, UserStatus.ACTIVE);

        List<CourseLesson> courseLessons = courseLessonRepo.findAllByCourse(course).stream()
                .filter(l -> !l.getStatus().equals(LessonStatus.DELETED))
                .toList();

        List<Long> lessonIds = courseLessons.stream().map(CourseLesson::getId).toList();
        List<Long> unitIds = courseLessons.stream()
                .flatMap(l -> l.getUnits().stream())
                .filter(u -> u.getStatus().equals(UnitStatus.ACTIVE))
                .map(Unit::getId)
                .distinct()
                .toList();

        List<ResCourseGroupDetails.ResCourseStudentDetail> studentDetails = new ArrayList<>();
        long totalResultSum = 0;

        for (Enrollment enrollment : enrollments) {
            Student s = enrollment.getStudent();

            //  Attendance
            long attended = 0;
            long markedCount = 0;
            if (!lessonIds.isEmpty()) {
                List<AttendanceRecord> records = attendanceRecordRepository.findAllByLesson_IdInAndStudent_IdIn(
                        lessonIds, List.of(s.getId()));
                markedCount = records.stream().filter(AttendanceRecord::getIsMarked).count();
                attended = records.stream()
                        .filter(r -> r.getIsMarked() && (r.getStatus() == AttendanceStatus.PRESENT || r.getStatus() == AttendanceStatus.LATE))
                        .count();
            }
            int attendancePct = (markedCount > 0) ? (int) ((attended * 100) / markedCount) : 100;

            // Calculate Result
            int resultPct = 0;
            if (!unitIds.isEmpty()) {
                Double avg = unitProgressRepository.getAverageProgressForStudentsAndUnits(
                        List.of(s.getId()), unitIds);
                resultPct = (avg != null) ? avg.intValue() : 0;
            }

            totalResultSum += resultPct;

            studentDetails.add(new ResCourseGroupDetails.ResCourseStudentDetail(
                    s.getUuid(),
                    s.getUser().getFirstName() + " " + s.getUser().getLastName(),
                    s.getUser().getAttachment() != null ? new ResAttachment(s.getUser().getAttachment()) : null,
                    attendancePct,
                    resultPct
            ));
        }

        studentDetails.sort(Comparator.comparing(ResCourseGroupDetails.ResCourseStudentDetail::getFullName));

        ResCourseGroupDetails response = new ResCourseGroupDetails();
        if (teacher != null) {
            response.setTeacherName(teacher.getUser().getFirstName() + " " + teacher.getUser().getLastName());
            if (teacher.getUser().getAttachment() != null) {
                response.setTeacherAttachment(new ResAttachment(teacher.getUser().getAttachment()));
            }
        }

        response.setStudents(studentDetails);
        response.setCourseAvgResult(enrollments.isEmpty() ? 0 : (int) (totalResultSum / enrollments.size()));

        return response;
    }

    @Override
    @Transactional
    public ResCreateCourse createCourse(ReqCourse request) {
        User currentUser = userService.getCurrentUser();
        Group group = groupRepository.findByUuid(request.getGroupId())
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.GROUP_NOT_FOUND.getKey()));

        School school = group.getBranch().getSchool();

        // Check if school is in Director's Org (Header is ignored for verification here)
        if (currentUser.getRole().equals(UserRole.SCHOOL_DIRECTOR)) {
            Organization org = schoolDirectorRepo.findByUser(currentUser).get().getOrganization();
            if (!school.getOrganization().equals(org)) {
                throw new PermissionForbidden(MessageKey.ORG_MISMATCH.getKey());
            }
        } else if (!currentUser.getRole().equals(UserRole.SYS_ADMIN)) {
            // For School Admins/Teachers, check header context
            if (!school.getUuid().equals(userScopeService.getCurrentUserSchoolUuid())) {
                throw new PermissionForbidden(MessageKey.SCHOOL_CONTEXT_REQUIRED.getKey());
            }
        }

        Course course = new Course();
        course.setName(request.getName());
        course.setDescription(request.getDescription());
        course.setSchool(school);
        course.setStatus(CourseStatus.ACTIVE);
        course.setGroup(group);
        course.setDuration(request.getCourseDuration() != null ? request.getCourseDuration() : 90);
        course.setCourseStartDate(request.getCourseStartDate() != null ? request.getCourseStartDate() : java.time.Instant.now());

        if (request.getBookIds() != null) {
            course.setLinkedBooks(schoolBookRepository.findAllByUuidIn(request.getBookIds()));
        }

        if (request.getPackageUuid() != null) {
            PaymentPackage pkg = paymentPackageRepo.findByUuid(request.getPackageUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.ORDER_NOT_FOUND.getKey()));
            course.setPaymentPackage(pkg);
        }

        Course savedCourse = courseRepo.save(course);
        return new ResCreateCourse("Course created", savedCourse.getUuid(), 1);
    }

    private Integer getLastLessonIndex(Course course) {
        if (course.getLessons() == null || course.getLessons().isEmpty()) {
            return 1;
        }
        return course.getLessons().stream()
                .filter(l -> l.getStatus() != LessonStatus.DELETED)
                .map(l -> {
                    String name = l.getName();
                    String[] parts = name.split(" ");
                    try {
                        return Integer.parseInt(parts[parts.length - 1]);
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .max(Integer::compare)
                .orElse(1);
    }


    @Transactional(readOnly = true)
    public Double calculateAverageCourseProgress(UUID courseUuid) {

        Course course = courseRepo.findByUuid(courseUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.COURSE_NOT_FOUND.getKey()));

        List<SchoolBook> linkedBooks = course.getLinkedBooks();
        Long schoolBookId = 0L;
        if (!CoreUtils.isEmpty(linkedBooks)) {
            schoolBookId = linkedBooks.get(0).getId();
        }
        List<Long> unitIds;
        if (schoolBookId == 0L) {
            unitIds = unitRepository.findUnitsByCourseUuid(courseUuid);
        } else {
            unitIds = unitRepository.findAllBySchoolBookIdAndStatus(schoolBookId, UnitStatus.ACTIVE).stream().map(Unit::getId).toList();
        }

        if (unitIds == null || unitIds.isEmpty()) {
            return 0.0;
        }

        List<Long> studentIds = enrollmentRepository.findAllByGroup_UuidAndStatus(
                        course.getGroup().getUuid(),
                        EnrollmentStatus.ONGOING
                )
                .stream()
                .map(enrollment -> enrollment.getStudent().getId())
                .toList();
        if (studentIds.isEmpty()) {
            return 0.0;
        }
        Optional<Student> student = studentRepo.findByUser(userService.getCurrentUser());
        Double avgProgress;
        if (student.isPresent()) {
            avgProgress = unitProgressRepository.getAverageProgressForStudentsAndUnitsV2(student.get().getId(), unitIds);

        } else {
            avgProgress = unitProgressRepository.getAverageProgressForStudentsAndUnits(studentIds, unitIds);
        }

        return (avgProgress != null) ? Math.round(avgProgress * 10.0) / 10.0 : 0.0;
    }


    @Override
    @Transactional
    public ResponseMessage updateCourse(UUID courseId, ReqCourse request) {
        Course course = courseRepo.findByUuid(courseId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.COURSE_NOT_FOUND.getKey()));
        User currentUser = userService.getCurrentUser();

        authToViewEntity.authorizeUponCourse(currentUser, course);

        if (request.getName() != null) course.setName(request.getName());
        if (request.getDescription() != null) course.setDescription(request.getDescription());

        if (request.getGroupId() != null && !request.getGroupId().equals(course.getGroup().getUuid())) {
            Group newGroup = groupRepository.findByUuid(request.getGroupId())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.GROUP_NOT_FOUND.getKey()));

            authToViewEntity.authorizeActionUponSchoolBroadAccess(newGroup.getBranch().getSchool());

            groupScheduleRepository.deactivateSchedulesForGroupAndCourse(course.getGroup(), course);
            course.setGroup(newGroup);
            reassignSchedulesToNewGroup(course, newGroup);
        }

        if (request.getBookIds() != null) {
            // Use the hierarchy-aware available books check
            List<SchoolBook> availableBooks = schoolBookRepository.findAvailableForSchool(course.getSchool().getUuid());
            Set<UUID> availableUuids = availableBooks.stream().map(SchoolBook::getUuid).collect(Collectors.toSet());
            List<SchoolBook> requestedBooks = schoolBookRepository.findAllByUuidIn(request.getBookIds());

            for (SchoolBook book : requestedBooks) {
                if (!availableUuids.contains(book.getUuid())) {
                    throw new ValidationException(MessageKey.ACCESS_DENIED.getKey());
                }
            }

            course.getLinkedBooks().clear();
            course.getLinkedBooks().addAll(requestedBooks);
        }

        if (request.getPackageUuid() != null) {
            PaymentPackage pkg = paymentPackageRepo.findByUuid(request.getPackageUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.ORDER_NOT_FOUND.getKey()));

            if (pkg.getSchool() != null) {
                authToViewEntity.authorizeActionUponSchoolBroadAccess(pkg.getSchool());
            }

            UUID courseOrgUuid = (course.getSchool().getOrganization() != null)
                    ? course.getSchool().getOrganization().getUuid() : null;

            UUID pkgOrgUuid = (pkg.getSchool() != null && pkg.getSchool().getOrganization() != null)
                    ? pkg.getSchool().getOrganization().getUuid() : null;

            course.setPaymentPackage(pkg);

//            boolean isGlobal = (pkg.getSchool() == null);
//            boolean isWithinSameOrg = (courseOrgUuid != null && courseOrgUuid.equals(pkgOrgUuid));

//            if (isGlobal || isWithinSameOrg) {
//            course.setPaymentPackage(pkg);

//            } else {
//                throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
//            }
        }

        if (request.getCourseDuration() != null) {
            course.setDuration(request.getCourseDuration());
        }
        courseRepo.save(course);
        return new ResponseMessage("Course updated successfully.");
    }

    private void reassignSchedulesToNewGroup(Course course, Group newGroup) {
        List<CourseLesson> activeLessons = course.getLessons().stream()
                .filter(l -> !l.getStatus().equals(LessonStatus.DELETED))
                .toList();

        for (CourseLesson lesson : activeLessons) {
            GroupSchedule schedule = groupScheduleRepository.findByLesson(lesson)
                    .orElse(new GroupSchedule());

            schedule.setGroup(newGroup);
            schedule.setLesson(lesson);
            schedule.setDueDate(lesson.getEndTime());
            schedule.setStatus(GroupScheduleStatus.ACTIVE);

            if (lesson.getUnits() != null && !lesson.getUnits().isEmpty()) {
                schedule.setUnit(lesson.getUnits().get(0));
            }

            groupScheduleRepository.save(schedule);

            if (lesson.getUnits() != null && !lesson.getUnits().isEmpty()) {
                scheduleService.createUnitsProgressesForNewSchedule(newGroup.getUuid(), lesson.getUnits());
            }
        }
    }


    @Override
    @Transactional
    public ResponseMessage finishCourse(UUID courseId, String endDate) {
        Course course = courseRepo.findByUuid(courseId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.COURSE_NOT_FOUND.getKey()));

        authToViewEntity.authorizeUponCourse(userService.getCurrentUser(), course);

        if (course.getStatus() == CourseStatus.FINISHED) {
            return new ResponseMessage("Course is already finished.");
        }

        LocalDate endLocalDate = endDate != null
                ? DateUtils.parseLocalDate(endDate)
                : LocalDate.now();

        LocalDate startLocalDate = course.getCourseStartDate()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        long days = ChronoUnit.DAYS.between(startLocalDate, endLocalDate);
        course.setDuration((int) days);
        if (!endLocalDate.isAfter(LocalDate.now())) {
            course.setStatus(CourseStatus.FINISHED);
        }
        courseRepo.save(course);

        if (course.getGroup() != null) {
            List<Enrollment> enrollments = enrollmentRepository.findAllByGroup_UuidAndStatus(
                    course.getGroup().getUuid(), EnrollmentStatus.ONGOING);

            for (Enrollment enrollment : enrollments) {
                Student student = enrollment.getStudent();
                student.setScore(0);
                studentRepo.save(student);
            }
        }

        course.getLessons().stream()
                .filter(l -> l.getStatus() != LessonStatus.DELETED)
                .forEach(lesson -> {
                    lesson.setStatus(LessonStatus.FINISHED);
                    groupScheduleRepository.findByLesson(lesson).ifPresent(s -> {
                        s.setStatus(GroupScheduleStatus.DELETED);
                        groupScheduleRepository.save(s);
                    });
                });

        return new ResponseMessage("Course finalized across organization branch.");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResCourseList> getCoursesByGroup(UUID groupUuid, CourseStatus status, Pageable pageable) {
        User user = userService.getCurrentUser();
        Group group = groupRepository.findByUuid(groupUuid).orElseThrow();

        authToViewEntity.authorizeActionUponSchoolBroadAccess(group.getBranch().getSchool());

        List<CourseStatus> statuses = (status != null) ? List.of(status) : List.of(CourseStatus.ACTIVE, CourseStatus.FINISHED);
        Page<Course> courses = courseRepo.findAllByGroup_UuidAndStatusIn(groupUuid, statuses, pageable);

        Map<UUID, CourseDurationSetting> durationSettingCache = new HashMap<>();
        return courses.map(course -> {
            long studentCount = enrollmentRepository.countByGroup_UuidAndStatusAndStudent_UserStatus(groupUuid, EnrollmentStatus.ONGOING, UserStatus.ACTIVE);
            ResCourseList resCourseList = new ResCourseList(course, course.getLessonCount(), "N/A", studentCount, calculateAverageCourseProgress(course.getUuid()));

            UUID courseSchoolUuid = (course.getSchool() != null) ? course.getSchool().getUuid() : null;

            CourseDurationSetting durationSetting = (courseSchoolUuid != null)
                    ? durationSettingCache.computeIfAbsent(courseSchoolUuid, this::resolveCourseDurationSetting)
                    : new CourseDurationSetting(false, 90);
            CourseDuration cd = computeCourseDuration(course, durationSetting);
            resCourseList.setCourseStartDate(course.getCourseStartDate());
            resCourseList.setCourseDueDate(cd.dueDate());
            resCourseList.setOverdue(cd.overdue());
            resCourseList.setDaysOverdue(cd.daysOverdue());
            resCourseList.setDurationProgressPercent(cd.durationProgressPercent());
            return resCourseList;
        });
    }

    @Override
    @Transactional
    public ResponseMessage deleteCourse(UUID courseId) {
        Course course = courseRepo.findByUuid(courseId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.COURSE_NOT_FOUND.getKey()));

        authToViewEntity.authorizeUponCourse(userService.getCurrentUser(), course);

        course.getLessons().forEach(lesson -> courseLessonServiceImpl.deleteLesson(lesson.getUuid()));
        courseLessonRepo.saveAll(course.getLessons());
        course.setStatus(CourseStatus.DELETED);
        courseRepo.save(course);

        return new ResponseMessage("Course deleted successfully!");
    }

    @Override
    @Transactional
    public ResponseMessage undeleteCourse(UUID courseId) {
        Course course = courseRepo.findByUuid(courseId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.COURSE_NOT_FOUND.getKey()));

        authToViewEntity.authorizeUponCourse(userService.getCurrentUser(), course);


        course.setStatus(CourseStatus.ACTIVE);

        course.getLessons().forEach(lesson -> {
            courseLessonServiceImpl.undeleteLesson(lesson.getUuid());
        });

        courseRepo.save(course);

        return new ResponseMessage("Course has been successfully restored.");
    }


    @Override
    @Transactional
    public ResponseMessage hardDeleteCourse(UUID courseId) {
//        Course course = courseRepo.findByUuid(courseId)
//                .orElseThrow(() -> new EntityNotFoundException(MessageKey.COURSE_NOT_FOUND.getKey()));
//
//        List<LessonRecording> recordings = lessonRecordingRepo.findAllByLesson_Course(course);
//        if (!recordings.isEmpty()) {
////            lessonRecordingRepo.deleteAll(recordings);
//        }
//
////        courseRepo.delete(course);
        return new ResponseMessage("can't delete the course.");
    }


}