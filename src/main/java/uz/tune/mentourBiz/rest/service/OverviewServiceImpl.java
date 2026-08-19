package uz.tune.mentourBiz.rest.service;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.Unit;
import uz.tune.mentourBiz.rest.domain.exercisesManagement.unit.exercise.postExercise.UnitProgress;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.CourseLesson;
import uz.tune.mentourBiz.rest.domain.schoolManagement.schedule.AttendanceRecord;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Enrollment;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.*;
import uz.tune.mentourBiz.rest.payload.ResLessonMemberDetail;
import uz.tune.mentourBiz.rest.payload.ResOverviewLesson;
import uz.tune.mentourBiz.rest.payload.res.attachment.ResAttachment;
import uz.tune.mentourBiz.rest.repository.course.CourseLessonRepo;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentRepository;
import uz.tune.mentourBiz.rest.repository.schedule.AttendanceRecordRepository;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;
import uz.tune.mentourBiz.rest.repository.unit.exercise.UnitProgressRepository;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OverviewServiceImpl {

    private final CourseLessonRepo lessonRepo;
    private final UserService userService;
    private final UserScopeService userScopeService;
    private final EnrollmentRepository enrollmentRepo;
    private final ValidationClass validator;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final UnitProgressRepository unitProgressRepository;
    private final SchoolRepo schoolRepo;

    @Transactional(readOnly = true)
    public List<ResOverviewLesson> getWeeklyLessons(Instant start, Instant end, UUID schoolUuid) {
        User user = userService.getCurrentUser();

        UUID resolvedId = userScopeService.resolveSchoolUuid(schoolUuid);
        School school = schoolRepo.findByUuid(resolvedId)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_NOT_FOUND.getKey()));

        Collection<UUID> filterSchoolUuids;
        if (resolvedId != null) {
            filterSchoolUuids = List.of(resolvedId);
        } else {
            filterSchoolUuids = userScopeService.getAuthorizedSchoolUuids();
        }

        List<LessonStatus> activeStatuses = Arrays.stream(LessonStatus.values())
                .filter(s -> s != LessonStatus.DELETED)
                .toList();

        // 3. Database-level filtering (much faster than in-memory stream filtering)
        List<CourseLesson> lessons = lessonRepo.findWithFilters(
                null,
                activeStatuses,
                filterSchoolUuids, // Pass the collection here
                null,
                null,
                start,
                end,
                Pageable.unpaged()
        ).getContent();

        // 4. Map to DTO (this part remains largely the same but simplified)
        return lessons.stream()
                .filter(l -> {
                    if (user.getRole() == UserRole.TEACHER) {
                        return l.getCourse().getGroup().getTeacher().getUser().getUuid().equals(user.getUuid());
                    }
                    return true;
                })
                .map(l -> {
                    long count = enrollmentRepo.countByGroup_UuidAndStatusAndStudent_UserStatus(
                            l.getCourse().getGroup().getUuid(), EnrollmentStatus.ONGOING, UserStatus.ACTIVE);

                    String teacherFullName = "N/A";
                    if(l.getCourse().getGroup().getTeacher() != null) {
                        User tUser = l.getCourse().getGroup().getTeacher().getUser();
                        teacherFullName = tUser.getFirstName() + " " + tUser.getLastName();
                    }

                    String latestUnitTitle = l.getUnits().isEmpty() ? null :
                            l.getUnits().stream().max(Comparator.comparing(Unit::getSortOrder)).map(Unit::getTitle).orElse(null);

                    return new ResOverviewLesson(
                            l.getUuid(), l.getName(), teacherFullName, l.getCourse().getGroup().getName(),
                            (int) count, l.getStartTime(), l.getEndTime(), l.getStatus(), latestUnitTitle,school
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ResLessonMemberDetail> getLessonMemberDetails(UUID lessonUuid) {
        CourseLesson lesson = lessonRepo.findByUuid(lessonUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.LESSON_NOT_FOUND.getKey()));

        validator.validateUserAccess(lesson.getCourse(), userService.getCurrentUser());

        UUID groupUuid = lesson.getCourse().getGroup().getUuid();

        List<Enrollment> enrollments = enrollmentRepo.findAllByGroup_UuidAndStatusAndStudent_User_Status(groupUuid, EnrollmentStatus.ONGOING, UserStatus.ACTIVE);
        if (enrollments.isEmpty()) return Collections.emptyList();

        List<Student> students = enrollments.stream().map(Enrollment::getStudent).toList();

        List<UUID> lessonUnitUuids = lesson.getUnits().stream().map(Unit::getUuid).toList();

        Map<UUID, AttendanceRecord> attendanceMap = attendanceRecordRepository.findAllByLessonIn(List.of(lesson))
                .stream().collect(Collectors.toMap(a -> a.getStudent().getUuid(), a -> a));

        Map<UUID, List<UnitProgress>> progressByStudent;
        if (!lessonUnitUuids.isEmpty()) {
            progressByStudent = unitProgressRepository.findAllByUnit_UuidInAndStudent_User_UuidIn(
                    new HashSet<>(lessonUnitUuids),
                    students.stream().map(s -> s.getUser().getUuid()).collect(Collectors.toSet())
            ).stream().collect(Collectors.groupingBy(p -> p.getStudent().getUser().getUuid()));
        } else {
            progressByStudent = new HashMap<>();
        }

        return students.stream().map(s -> {
                    List<UnitProgress> progresses = progressByStudent.getOrDefault(s.getUser().getUuid(), Collections.emptyList());
                    int avgProgress = progresses.isEmpty() ? 0 :
                            (int) progresses.stream().mapToInt(UnitProgress::getProgressPercentage).average().orElse(0);

                    AttendanceRecord ar = attendanceMap.get(s.getUuid());

                    return new ResLessonMemberDetail(
                            s.getUuid(),
                            s.getUser().getUuid(),
                            s.getUser().getFirstName() + " " + s.getUser().getLastName(),
                            s.getUser().getAttachment() != null ? new ResAttachment(s.getUser().getAttachment()) : null,
                            avgProgress,
                            ar != null ? ar.getStatus() : AttendanceStatus.NOT_MARKED,
                            ar != null && ar.getIsMarked()
                    );
                })
                .sorted(Comparator.comparing(ResLessonMemberDetail::getFullName))
                .toList();
    }
}