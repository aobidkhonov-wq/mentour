package uz.tune.mentourBiz.rest.service;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.Course;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.CourseLesson;
import uz.tune.mentourBiz.rest.domain.schoolManagement.schedule.AttendanceRecord;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Enrollment;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.enums.AttendanceStatus;
import uz.tune.mentourBiz.rest.enums.EnrollmentStatus;
import uz.tune.mentourBiz.rest.enums.LessonStatus;
import uz.tune.mentourBiz.rest.enums.UserStatus;
import uz.tune.mentourBiz.rest.payload.res.ResAttendanceGrid;
import uz.tune.mentourBiz.rest.payload.res.ResOverallAttendance;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;
import uz.tune.mentourBiz.rest.repository.course.CourseLessonRepo;
import uz.tune.mentourBiz.rest.repository.course.CourseRepo;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentRepository;
import uz.tune.mentourBiz.rest.repository.schedule.AttendanceRecordRepository;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.service.helper.AuthToViewEntity;
import uz.tune.mentourBiz.rest.service.user.UserService;
import uz.tune.mentourBiz.utils.AttendanceUtils;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceGridService {

    private final CourseRepo courseRepo;
    private final CourseLessonRepo courseLessonRepo;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final AuthToViewEntity authToViewEntity;
    private final UserService userService;
    private final StudentRepo studentRepo;

    @Transactional(readOnly = true)
    public ResAttendanceGrid getMonthlyGrid(UUID courseUuid, Integer year, Integer month) {
        ZoneId zoneId = ZoneId.of("Asia/Tashkent");
        Instant start = null;
        Instant end = null;

        if (year != null && month != null) {
            YearMonth yearMonth = YearMonth.of(year, month);
            start = yearMonth.atDay(1).atStartOfDay(zoneId).toInstant();
            end = yearMonth.atEndOfMonth().atTime(LocalTime.MAX).atZone(zoneId).toInstant();
        }

        Course course = courseRepo.findByUuid(courseUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.COURSE_NOT_FOUND.getKey()));

        authToViewEntity.authorizeUponCourse(userService.getCurrentUser(), course);

        List<CourseLesson> allCourseLessons = courseLessonRepo.findAllByCourse(course).stream()
                .filter(l -> !l.getStatus().equals(LessonStatus.DELETED))
                .sorted(Comparator.comparing(CourseLesson::getStartTime))
                .toList();

        LocalDate courseStart = allCourseLessons.isEmpty() ? null : LocalDate.ofInstant(allCourseLessons.get(0).getStartTime(), zoneId);
        LocalDate courseEnd = allCourseLessons.isEmpty() ? null : LocalDate.ofInstant(allCourseLessons.get(allCourseLessons.size() - 1).getStartTime(), zoneId);

        Pageable sortedByStartTime = PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.ASC, "startTime"));

        List<CourseLesson> lessons = courseLessonRepo.findWithFilters(
                null, List.of(LessonStatus.STUDENT_APP, LessonStatus.FINISHED), null, null, List.of(course.getUuid()), start, end, sortedByStartTime
        ).getContent();

        List<Enrollment> enrollments = enrollmentRepository.findAllByGroup_UuidAndStatusAndStudent_User_Status(
                course.getGroup().getUuid(), EnrollmentStatus.ONGOING, UserStatus.ACTIVE);

        List<Long> studentIds = enrollments.stream().map(e -> e.getStudent().getId()).toList();
        List<Long> lessonIds = lessons.stream().map(CourseLesson::getId).toList();

        // Bitta student+dars uchun dublikat yozuv bo'lishi mumkin — merge funksiyasiz toMap yiqilardi.
        Map<String, AttendanceRecord> attendanceMap = lessons.isEmpty() ? Map.of() :
                attendanceRepository.findAllByLesson_IdInAndStudent_IdIn(lessonIds, studentIds)
                        .stream().collect(Collectors.toMap(
                                a -> a.getStudent().getUuid() + ":" + a.getLesson().getUuid(),
                                a -> a,
                                AttendanceUtils::canonical));

        List<ResAttendanceGrid.AttendanceGridHeader> headers = lessons.stream()
                .map(l -> new ResAttendanceGrid.AttendanceGridHeader(
                        LocalDate.ofInstant(l.getStartTime(), zoneId),
                        l.getName(),
                        l.getUuid()))
                .collect(Collectors.toList());

        List<ResAttendanceGrid.StudentAttendanceRow> rows = enrollments.stream().map(e -> {
            Student s = e.getStudent();
            List<ResAttendanceGrid.AttendanceCell> cells = new ArrayList<>();

            for (CourseLesson lesson : lessons) {
                AttendanceRecord record = attendanceMap.get(s.getUuid() + ":" + lesson.getUuid());
                AttendanceStatus status;
                if (record != null && record.getStatus() != null) {
                    status = record.getStatus();
                } else if (record == null
                        && e.getCreatedAt() != null
                        && lesson.getCreatedAt() != null
                        && lesson.getStartTime().isBefore(e.getCreatedAt())
                        && lesson.getCreatedAt().isBefore(e.getCreatedAt())) {
                    // the lesson already existed when the student joined (transfer / late enrollment),
                    // so the student really was not part of it. Lessons created afterwards with a past
                    // date fall through to NOT_MARKED.
                    status = null;
                } else {
                    status = AttendanceStatus.NOT_MARKED;
                }

                cells.add(new ResAttendanceGrid.AttendanceCell(
                        lesson.getUuid(),
                        status,
                        record != null ? record.getIsMarked() : false,
                        record != null ? record.getNote() : null
                ));
            }

            return new ResAttendanceGrid.StudentAttendanceRow(
                    s.getUser().getUuid(),
                    s.getUser().getFirstName() + " " + s.getUser().getLastName(),
                    s.getUser().getUsername(),
                    s.getUser().getAttachment() != null ? new ResAttachment(s.getUser().getAttachment()) : null,
                    cells);
        }).collect(Collectors.toList());

        return new ResAttendanceGrid(headers, rows, courseStart, courseEnd);
    }



    @Transactional(readOnly = true)
    public ResOverallAttendance getOverallAttendance(UUID groupUuid, UUID studentUuid, LocalDate date) {
        ZoneId zoneId = ZoneId.of("Asia/Tashkent");
        Instant until = (date != null)
                ? date.atTime(LocalTime.MAX).atZone(zoneId).toInstant()
                : Instant.now();

        Student student = studentRepo.findByUuid(studentUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));
        authToViewEntity.authorizeActionUponStudent(student);

        List<AttendanceRecord> records = attendanceRepository.findActiveForStudentInGroup(groupUuid, studentUuid, until);

        long total = records.size();
        long attended = records.stream()
                .filter(r -> r.getStatus() == AttendanceStatus.PRESENT || r.getStatus() == AttendanceStatus.LATE)
                .count();
        double percentage = (total == 0) ? 0.0 : Math.round(attended * 10000.0 / total) / 100.0;

        return new ResOverallAttendance(studentUuid, groupUuid, total, attended, percentage);
    }
}
