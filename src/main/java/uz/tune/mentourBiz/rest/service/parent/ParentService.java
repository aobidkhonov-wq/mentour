package uz.tune.mentourBiz.rest.service.parent;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.PaymentPackage;
import uz.tune.mentourBiz.rest.domain.StudentParentContact;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.Course;
import uz.tune.mentourBiz.rest.domain.schoolManagement.schedule.AttendanceRecord;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Enrollment;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.postExercise.UnitProgress;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Teacher;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.*;
import uz.tune.mentourBiz.rest.payload.req.parent.ReqChangeParentPassword;
import uz.tune.mentourBiz.rest.payload.req.parent.ReqParentSignIn;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;
import uz.tune.mentourBiz.rest.payload.res.auth.ResSignIn;
import uz.tune.mentourBiz.rest.payload.res.course.ResCourseView;
import uz.tune.mentourBiz.rest.payload.res.level.ResLevel;
import uz.tune.mentourBiz.rest.payload.res.parent.ResChildCourseStat;
import uz.tune.mentourBiz.rest.payload.res.parent.ResChildOverview;
import uz.tune.mentourBiz.rest.payload.res.parent.ResChildSchool;
import uz.tune.mentourBiz.rest.payload.res.parent.ResChildStats;
import uz.tune.mentourBiz.rest.payload.res.parent.ResChildTeacher;
import uz.tune.mentourBiz.rest.service.lessons.CourseService;
import uz.tune.mentourBiz.rest.repository.StudentParentContactRepository;
import uz.tune.mentourBiz.rest.repository.course.CourseRepo;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentRepository;
import uz.tune.mentourBiz.rest.repository.schedule.AttendanceRecordRepository;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.repository.unit.exercise.UnitProgressRepository;
import uz.tune.mentourBiz.rest.repository.user.UserRepo;
import uz.tune.mentourBiz.rest.service.user.UserService;
import uz.tune.mentourBiz.rest.service.util.impl.AuthService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParentService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final UserService userService;
    private final StudentParentContactRepository parentContactRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepo courseRepo;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final UnitProgressRepository unitProgressRepository;
    private final CourseService courseService;
    private final StudentRepo studentRepo;

    /**
     * Parents log in with their phone number. If a PARENT user already exists, the password is
     * verified. Otherwise, if the phone is registered as a parent contact of any student, a PARENT
     * account is created on the fly with the supplied password (first login = registration).
     */
    @Transactional
    public ResSignIn signIn(ReqParentSignIn request) {
        String phone = request.getPhoneNumber().trim();

        User user = userRepo.findByUsernameAndStatus(phone, UserStatus.ACTIVE).orElse(null);

        if (user != null) {
            if (user.getRole() != UserRole.PARENT) {
                throw new ValidationException(MessageKey.PARENT_ACCESS_ONLY.getKey());
            }
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new ValidationException(MessageKey.PASSWORD_IS_INCORRECT.getKey());
            }
        } else {
            List<StudentParentContact> contacts = parentContactRepository.findAllByPhoneNumberAndIsActiveTrue(phone);
            if (contacts.isEmpty()) {
                throw new ValidationException(MessageKey.PARENT_NOT_FOUND.getKey());
            }
            StudentParentContact contact = contacts.get(0);
            user = new User();
            user.setUsername(phone);
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(UserRole.PARENT);
            user.setStatus(UserStatus.ACTIVE);
            user.setFirstName((contact.getParentName() != null && !contact.getParentName().isBlank())
                    ? contact.getParentName() : "Parent");
            user.setLastName("");
            user.setLang(contact.getLanguage() != null ? contact.getLanguage() : Lang.UZB);
            user = userRepo.save(user);
        }

        user.setLastActiveAt(Instant.now());
        userRepo.save(user);
        return new ResSignIn(authService.authenticateUser(user));
    }

    @Transactional
    public ResponseMessage changePassword(ReqChangeParentPassword request) {
        User parent = currentParent();
        if (!passwordEncoder.matches(request.getCurrentPassword(), parent.getPassword())) {
            throw new ValidationException(MessageKey.PASSWORD_IS_INCORRECT.getKey());
        }
        parent.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepo.save(parent);
        return new ResponseMessage("Password updated successfully.");
    }

    @Transactional(readOnly = true)
    public List<ResChildStats> getChildrenDashboard() {
        User parent = currentParent();

        List<StudentParentContact> contacts = parentContactRepository.findAllByPhoneNumberAndIsActiveTrue(parent.getUsername());

        // Distinct children (one phone may be linked to several students).
        Map<Long, Student> children = new LinkedHashMap<>();
        for (StudentParentContact c : contacts) {
            if (c.getStudent() != null) children.putIfAbsent(c.getStudent().getId(), c.getStudent());
        }

        return children.values().stream().map(this::buildChildStats).collect(Collectors.toList());
    }

    /**
     * One-screen overview per child: school and teacher objects, at-risk flag, overall
     * attendance, average score, homework counts and the next payment date.
     */
    @Transactional(readOnly = true)
    public List<ResChildOverview> getChildrenOverview() {
        User parent = currentParent();

        List<StudentParentContact> contacts = parentContactRepository.findAllByPhoneNumberAndIsActiveTrue(parent.getUsername());

        Map<Long, Student> children = new LinkedHashMap<>();
        Map<Long, String> parentNames = new HashMap<>();
        for (StudentParentContact c : contacts) {
            if (c.getStudent() != null) {
                children.putIfAbsent(c.getStudent().getId(), c.getStudent());
                if (c.getParentName() != null && !c.getParentName().isBlank()) {
                    parentNames.putIfAbsent(c.getStudent().getId(), c.getParentName());
                }
            }
        }

        String fallbackParentName = (parent.getFirstName() + " " + parent.getLastName()).trim();
        return children.values().stream()
                .map(s -> buildChildOverview(s, parentNames.getOrDefault(s.getId(), fallbackParentName)))
                .collect(Collectors.toList());
    }

    /**
     * Full course dashboard(s) of one of the parent's children (lessons, the child's attendance
     * per lesson, progress, course duration). Returns one ResCourseView per active course.
     */
    @Transactional(readOnly = true)
    public List<ResCourseView> getChildCourseDashboards() {
        User parent = currentParent();

        Student child = parentContactRepository.findAllByPhoneNumberAndIsActiveTrue(parent.getUsername())
                .stream()
                .map(StudentParentContact::getStudent)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey()));

        UUID childUserUuid = child.getUser().getUuid();

        return enrollmentRepository.findAllByStudent_User_UuidAndStatus(childUserUuid, EnrollmentStatus.ONGOING)
                .stream()
                .map(Enrollment::getGroup)
                .filter(Objects::nonNull)
                .flatMap(g -> courseRepo.findAllByGroupAndStatus(g, CourseStatus.ACTIVE).stream())
                .collect(Collectors.toMap(Course::getId, c -> c, (a, b) -> a))
                .values().stream()
                .map(c -> courseService.getCourseViewForStudent(c.getUuid(), childUserUuid))
                .collect(Collectors.toList());
    }

    // ---- helpers ----

    private User currentParent() {
        User user = userService.getCurrentUser();
        if (user.getRole() != UserRole.PARENT) {
            throw new PermissionForbidden(MessageKey.PARENT_ACCESS_ONLY.getKey());
        }
        return user;
    }

    private ResChildStats buildChildStats(Student student) {
        UUID userUuid = student.getUser().getUuid();
        String fullName = student.getUser().getFirstName() + " " + student.getUser().getLastName();
        String schoolName = (student.getSchool() != null) ? student.getSchool().getName() : null;

        List<AttendanceRecord> records = attendanceRecordRepository.findAllByStudent_User_Uuid(userUuid);
        List<Course> courses = activeCourses(userUuid);

        List<ResChildCourseStat> courseStats = new ArrayList<>();
        long totalMarked = 0, totalAttended = 0;
        int scoreSum = 0, scoreCount = 0;

        for (Course course : courses) {
            CourseNumbers n = computeCourseNumbers(course, records, userUuid);
            int attendancePct = (n.marked() > 0) ? (int) (n.attended() * 100 / n.marked()) : 0;
            int scorePct = (n.scorePct() != null) ? n.scorePct() : 0;

            courseStats.add(new ResChildCourseStat(course.getUuid(), course.getName(), attendancePct, scorePct));

            totalMarked += n.marked();
            totalAttended += n.attended();
            if (n.scorePct() != null) {
                scoreSum += scorePct;
                scoreCount++;
            }
        }

        int overallAttendance = (totalMarked > 0) ? (int) (totalAttended * 100 / totalMarked) : 0;
        int overallScore = (scoreCount > 0) ? (scoreSum / scoreCount) : 0;

        return new ResChildStats(student.getUuid(), fullName, schoolName, overallAttendance, overallScore, courseStats);
    }

    private ResChildOverview buildChildOverview(Student student, String parentName) {
        UUID userUuid = student.getUser().getUuid();
        String fullName = student.getUser().getFirstName() + " " + student.getUser().getLastName();

        List<AttendanceRecord> records = attendanceRecordRepository.findAllByStudent_User_Uuid(userUuid);
        List<Course> courses = activeCourses(userUuid);

        long totalMarked = 0, totalAttended = 0;
        int scoreSum = 0, scoreCount = 0;
        long homeworkCount = 0, completedHomeworkCount = 0;

        for (Course course : courses) {
            CourseNumbers n = computeCourseNumbers(course, records, userUuid);
            totalMarked += n.marked();
            totalAttended += n.attended();
            homeworkCount += n.totalUnits();
            completedHomeworkCount += n.completedUnits();
            if (n.scorePct() != null) {
                scoreSum += n.scorePct();
                scoreCount++;
            }
        }

        Group group = enrollmentRepository
                .findAllByStudent_User_UuidAndStatus(userUuid, EnrollmentStatus.ONGOING)
                .stream()
                .map(Enrollment::getGroup)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        Teacher teacher = (group != null && group.getTeacher() != null && group.getTeacher().getUser() != null)
                ? group.getTeacher() : null;

        // School-wide rank by score, same formula as the student home profile.
        Integer ranking = (student.getSchool() != null)
                ? studentRepo.countStudentsWithHigherScoreInSchool(student.getSchool().getUuid(), student.getScore()) + 1
                : null;

        User childUser = student.getUser();

        return ResChildOverview.builder()
                .studentUuid(student.getUuid())
                .fullName(fullName)
                .status(childUser.getStatus())
                .studentPhoto((childUser.getAttachment() != null) ? new ResAttachment(childUser.getAttachment()) : null)
                .parentName(parentName)
                .studentBalance(student.getCurrentBalance())
                .groupName((group != null) ? group.getName() : null)
                .level((group != null && group.getLevel() != null) ? new ResLevel(group.getLevel()) : null)
                .coins(student.getCoins())
                .score(student.getScore())
                .ranking(ranking)
                .isAtRisk(Boolean.TRUE.equals(student.getIsAtRisk()))
                .school((student.getSchool() != null) ? new ResChildSchool(student.getSchool()) : null)
                .teacher((teacher != null) ? new ResChildTeacher(teacher) : null)
                .attendancePercentage((totalMarked > 0) ? (int) (totalAttended * 100 / totalMarked) : 0)
                .averageScorePercentage((scoreCount > 0) ? (scoreSum / scoreCount) : 0)
                .homeworkCount(homeworkCount)
                .completedHomeworkCount(completedHomeworkCount)
                .nextPaymentDate(nextPaymentDate(student, courses))
                .paymentAmount(paymentAmount(courses))
                .build();
    }

    private List<Course> activeCourses(UUID userUuid) {
        return enrollmentRepository
                .findAllByStudent_User_UuidAndStatus(userUuid, EnrollmentStatus.ONGOING)
                .stream()
                .map(Enrollment::getGroup)
                .filter(Objects::nonNull)
                .flatMap(g -> courseRepo.findAllByGroupAndStatus(g, CourseStatus.ACTIVE).stream())
                .collect(Collectors.toMap(Course::getId, c -> c, (a, b) -> a))
                .values().stream().toList();
    }

    // Per-course attendance and homework numbers; scorePct is null when the child has no
    // unit progress in the course (so it is excluded from the overall average).
    private record CourseNumbers(long marked, long attended, int totalUnits, int completedUnits, Integer scorePct) {}

    private CourseNumbers computeCourseNumbers(Course course, List<AttendanceRecord> records, UUID userUuid) {
        List<AttendanceRecord> courseRecords = records.stream()
                .filter(r -> r.getLesson() != null && r.getLesson().getCourse() != null
                        && Objects.equals(r.getLesson().getCourse().getId(), course.getId()))
                .toList();
        long marked = courseRecords.stream().filter(AttendanceRecord::getIsMarked).count();
        long attended = courseRecords.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsMarked())
                        && (r.getStatus() == AttendanceStatus.PRESENT || r.getStatus() == AttendanceStatus.LATE))
                .count();

        Set<UUID> unitUuids = (course.getLessons() == null) ? Set.of() : course.getLessons().stream()
                .filter(l -> !l.getStatus().equals(LessonStatus.DELETED))
                .filter(l -> l.getUnits() != null)
                .flatMap(l -> l.getUnits().stream())
                .map(Unit::getUuid)
                .collect(Collectors.toSet());

        List<UnitProgress> progresses = unitUuids.isEmpty() ? List.of()
                : unitProgressRepository.findAllByUnit_UuidInAndStudent_User_Uuid(unitUuids, userUuid);
        Integer scorePct = progresses.isEmpty() ? null
                : (int) progresses.stream().mapToInt(UnitProgress::getProgressPercentage).average().orElse(0);

        return new CourseNumbers(marked, attended, unitUuids.size(), progresses.size(), scorePct);
    }

    /**
     * The next payment date is derived from the payment packages of the child's active courses:
     * each package carries a due day-of-month, and the nearest upcoming occurrence wins.
     * Null when billing is not activated for the student or no package has a due day.
     */
    private LocalDate nextPaymentDate(Student student, List<Course> courses) {
        if (!student.isPaymentActivated()) return null;
        return courses.stream()
                .map(Course::getPaymentPackage)
                .filter(Objects::nonNull)
                .map(PaymentPackage::getPaymentDueDate)
                .filter(Objects::nonNull)
                .map(ParentService::nextOccurrenceOfDay)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    // Total monthly amount across the payment packages of the child's active courses;
    // null when no course carries a package.
    private Long paymentAmount(List<Course> courses) {
        List<Long> prices = courses.stream()
                .map(Course::getPaymentPackage)
                .filter(Objects::nonNull)
                .map(PaymentPackage::getPrice)
                .filter(Objects::nonNull)
                .toList();
        return prices.isEmpty() ? null : prices.stream().mapToLong(Long::longValue).sum();
    }

    private static LocalDate nextOccurrenceOfDay(int dueDay) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Tashkent"));
        LocalDate candidate = today.withDayOfMonth(Math.min(dueDay, today.lengthOfMonth()));
        if (candidate.isBefore(today)) {
            LocalDate nextMonth = today.plusMonths(1);
            candidate = nextMonth.withDayOfMonth(Math.min(dueDay, nextMonth.lengthOfMonth()));
        }
        return candidate;
    }
}
