package uz.tune.mentourBiz.rest.service.group.schedule.impl;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.rest.domain.SchoolDirector;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.Course;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.CourseLesson;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Enrollment;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.*;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;
import uz.tune.mentourBiz.rest.payload.studentRes.res.lesson.ResMarkCell;
import uz.tune.mentourBiz.rest.payload.studentRes.res.lesson.ResMarksDashboard;
import uz.tune.mentourBiz.rest.payload.studentRes.res.lesson.ResMarksHeader;
import uz.tune.mentourBiz.rest.payload.studentRes.res.lesson.ResStudentMarksRow;
import uz.tune.mentourBiz.rest.repository.SchoolDirectorRepo;
import uz.tune.mentourBiz.rest.repository.course.CourseLessonRepo;
import uz.tune.mentourBiz.rest.repository.course.CourseRepo;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentRepository;
import uz.tune.mentourBiz.rest.repository.schedule.AttendanceRecordRepository;
import uz.tune.mentourBiz.rest.repository.schedule.GroupScheduleRepository;
import uz.tune.mentourBiz.rest.repository.unit.VocabularyAnswerRepository;
import uz.tune.mentourBiz.rest.repository.unit.exercise.ExerciseAnswersRepository;
import uz.tune.mentourBiz.rest.service.exercise.impl.ProgressService;
import uz.tune.mentourBiz.rest.service.group.schedule.MarksService;
import uz.tune.mentourBiz.rest.service.user.UserService;
import uz.tune.mentourBiz.rest.service.user.impl.UserScopeServiceImpl;
import uz.tune.mentourBiz.utils.AttendanceUtils;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MarksServiceImpl implements MarksService {

    private final UserService userService;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final GroupScheduleRepository groupScheduleRepository;
    private final CourseRepo courseRepo;
    private final UserScopeServiceImpl userScopeServiceImpl;
    private final CourseLessonRepo courseLessonRepo;
    private final SchoolDirectorRepo schoolDirectorRepo;
    private final ProgressService progressService;
    private final ExerciseAnswersRepository exerciseAnswersRepository;
    private final VocabularyAnswerRepository vocabularyAnswerRepository;

    @Override
    @Transactional(readOnly = true)
    public ResMarksDashboard getMarksForGroup(UUID courseUuid, Integer year, Integer month) {
        ZoneId zoneId = ZoneId.of("Asia/Tashkent");
        LocalDate now = LocalDate.now(zoneId);
        Instant startInstant = null;
        Instant endInstant = null;

        if (year != null && month != null) {
            YearMonth yearMonth = YearMonth.of(year, month);
            startInstant = yearMonth.atDay(1).atStartOfDay(zoneId).toInstant();
            endInstant = yearMonth.atEndOfMonth().atTime(LocalTime.MAX).atZone(zoneId).toInstant();
        }

        Course course = courseRepo.findByUuid(courseUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.COURSE_NOT_FOUND.getKey()));

        User user = userService.getCurrentUser();
        if (user.getRole().equals(UserRole.SCHOOL_DIRECTOR)) {
            SchoolDirector director = schoolDirectorRepo.findByUser(user).get();
            if (!course.getSchool().getOrganization().equals(director.getOrganization())) {
                throw new PermissionForbidden(MessageKey.ACCESS_DENIED.getKey());
            }
        } else {
            userScopeServiceImpl.getCurrentUserSchoolUuid();
        }

        List<CourseLesson> allLessons = courseLessonRepo.findAllByCourse(course).stream()
                .filter(l -> !l.getStatus().equals(LessonStatus.DELETED))
                .sorted(Comparator.comparing(CourseLesson::getStartTime))
                .toList();

        LocalDate courseStart = allLessons.isEmpty() ? null : LocalDate.ofInstant(allLessons.get(0).getStartTime(), zoneId);
        LocalDate courseEnd = allLessons.isEmpty() ? null : LocalDate.ofInstant(allLessons.get(allLessons.size() - 1).getStartTime(), zoneId);

        List<Enrollment> enrollments = enrollmentRepository.findAllByGroup_UuidAndStatusAndStudent_User_Status(
                course.getGroup().getUuid(), EnrollmentStatus.ONGOING, UserStatus.ACTIVE);
        List<Long> studentIds = enrollments.stream()
                .filter(e -> e.getStudent().getUser().getStatus() == UserStatus.ACTIVE)
                .map(e -> e.getStudent().getId())
                .toList();

        List<CourseLesson> lessons = courseLessonRepo.findWithFilters(
                null,  List.of(LessonStatus.STUDENT_APP, LessonStatus.FINISHED), null, null, List.of(course.getUuid()), startInstant, endInstant, Pageable.unpaged()
        ).getContent();

        List<Long> lessonIds = lessons.stream().map(CourseLesson::getId).toList();
        List<Unit> allUnitsInMonth = lessons.stream()
                .flatMap(l -> l.getUnits().stream())
                .distinct()
                .toList();
        // Foiz endi saqlangan (stale) UnitProgress dan emas, jonli (live) hisoblanadi —
        // ProgressService.calculateTotalPossible/calculateTotalEarned bilan, bu summary/detal
        // API'lardagi formulaning aynan o'zi (single source of truth).
        UUID schoolUuid = course.getSchool().getUuid();
        List<UUID> unitUuids = allUnitsInMonth.stream().map(Unit::getUuid).toList();

        Map<Long, Long> possibleByUnitId = new HashMap<>();
        for (Unit u : allUnitsInMonth) {
            possibleByUnitId.put(u.getId(), progressService.calculateTotalPossible(u, schoolUuid));
        }

        Map<String, Long> earnedMap = new HashMap<>();
        if (!studentIds.isEmpty() && !unitUuids.isEmpty()) {
            for (Object[] row : exerciseAnswersRepository.sumEarnedGroupedByStudentsAndUnits(studentIds, unitUuids)) {
                earnedMap.merge(row[0] + ":" + row[1], ((Number) row[2]).longValue(), Long::sum);
            }
            for (Object[] row : vocabularyAnswerRepository.sumEarnedVocabGroupedByStudentsAndUnits(studentIds, unitUuids)) {
                earnedMap.merge(row[0] + ":" + row[1], ((Number) row[2]).longValue(), Long::sum);
            }
        }

        Map<String, Integer> progressMap = new HashMap<>();
        for (Unit u : allUnitsInMonth) {
            long possible = possibleByUnitId.getOrDefault(u.getId(), 0L);
            for (Long sid : studentIds) {
                long earned = earnedMap.getOrDefault(sid + ":" + u.getUuid(), 0L);
                progressMap.put(sid + ":" + u.getId(), progressService.toPercentage(earned, possible));
            }
        }

        // Bitta student+dars uchun dublikat yozuv bo'lishi mumkin, shuning uchun avval
        // AttendanceUtils.canonical bilan bittaga keltiramiz (merge funksiyasiz toMap yiqilardi).
        Map<String, AttendanceStatus> attendanceMap = lessonIds.isEmpty() ? Map.of() :
                attendanceRepository.findAllByLesson_IdInAndStudent_IdIn(lessonIds, studentIds)
                        .stream().collect(Collectors.toMap(
                                a -> a.getStudent().getId() + ":" + a.getLesson().getId(),
                                a -> a,
                                AttendanceUtils::canonical))
                        .entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getStatus()));

        ResMarksDashboard dashboard = new ResMarksDashboard();
        dashboard.setCourseStartDate(courseStart);
        dashboard.setCourseEndDate(courseEnd);

        List<ResMarksHeader> headers = new ArrayList<>();
        for (CourseLesson lesson : lessons) {
            LocalDate date = LocalDate.ofInstant(lesson.getStartTime(), zoneId);
            List<Unit> activeUnits = activeUnitsOf(lesson);
            if (activeUnits.isEmpty()) {
                headers.add(new ResMarksHeader(date, lesson.getName(), null, lesson.getUuid()));
                continue;
            }
            for (Unit unit : activeUnits) {
                headers.add(new ResMarksHeader(date, unit.getTitle(), unit.getUuid(), lesson.getUuid()));
            }
        }
        dashboard.setHeaders(headers);

        dashboard.setStudentRows(enrollments.stream().map(e -> {
            Student student = e.getStudent();
            ResStudentMarksRow row = new ResStudentMarksRow();
            row.setStudentUuid(student.getUser().getUuid());
            row.setUserName(student.getUser().getUsername());
            if (student.getUser().getAttachment() != null) {
                row.setAttachment(new ResAttachment(student.getUser().getAttachment()));
            }
            row.setStudentFullName(student.getUser().getFirstName() + " " + student.getUser().getLastName());
            int countForAverage = 0;
            int sumForAverage = 0;
            List<ResMarkCell> marks = new ArrayList<>();
            for (CourseLesson lesson : lessons) {
                AttendanceStatus attStatus = attendanceMap.getOrDefault(student.getId() + ":" + lesson.getId(), AttendanceStatus.NOT_MARKED);
                LocalDate date = LocalDate.ofInstant(lesson.getStartTime(), zoneId);
                List<Unit> activeUnits = activeUnitsOf(lesson);
                if (activeUnits.isEmpty()) {
                    // Baholanadigan unit yo'q, lekin davomat baribir shu darsga tegishli:
                    // score = null bo'lgan katak chiqadi va o'rtacha ballga qo'shilmaydi.
                    marks.add(new ResMarkCell(null, date, null, attStatus, lesson.getUuid()));
                    continue;
                }
                for (Unit unit : activeUnits) {
                    Integer progress = progressMap.getOrDefault(student.getId() + ":" + unit.getId(), 0);
                    marks.add(new ResMarkCell(unit.getUuid(), date, progress, attStatus, lesson.getUuid()));
                    if (!date.isAfter(now)) {
                        sumForAverage += (progress != null ? progress : 0);
                        countForAverage++;
                    }
                }
            }
            row.setMarks(marks);

            if (countForAverage > 0) {
                row.setOverallScore((int) Math.round((double) sumForAverage / countForAverage));
            } else {
                row.setOverallScore(0);
            }
            return row;
        }).toList());

        return dashboard;
    }

    private List<Unit> activeUnitsOf(CourseLesson lesson) {
        if (lesson.getUnits() == null) return List.of();
        return lesson.getUnits().stream()
                .filter(u -> u.getStatus() == UnitStatus.ACTIVE)
                .toList();
    }
}