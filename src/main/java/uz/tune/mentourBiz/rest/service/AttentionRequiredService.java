package uz.tune.mentourBiz.rest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.tune.mentourBiz.rest.domain.ResAttentionDetail;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.CourseLesson;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Enrollment;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Group;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.AttentionType;
import uz.tune.mentourBiz.rest.enums.EnrollmentStatus;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.repository.SchoolAcademicConfigRepo;
import uz.tune.mentourBiz.rest.repository.course.CourseLessonRepo;
import uz.tune.mentourBiz.rest.repository.course.CourseRepo;
import uz.tune.mentourBiz.rest.repository.group.GroupRepository;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentRepository;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor

public class AttentionRequiredService {

    private final StudentRepo studentRepo;
    private final GroupRepository groupRepo;
    private final CourseLessonRepo lessonRepo;
    private final CourseRepo courseRepo;
    private final UserScopeService userScopeService;
    private final UserService userService;
    private final SchoolAcademicConfigRepo schoolAcademicConfigRepo;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public Map<String, Long> getSummaryCounts() {
        User user = userService.getCurrentUser();
        Collection<UUID> schoolUuids = userScopeService.getAuthorizedSchoolUuids();
        UUID tUuid = (user.getRole() == UserRole.TEACHER) ? user.getUuid() : null;
        Instant now = Instant.now();

        Map<String, Long> counts = new HashMap<>();

        if (user.getRole() != UserRole.TEACHER) {
            counts.put("OVERDUE", studentRepo.countOverdueMulti(schoolUuids));
            counts.put("UNASSIGNED", studentRepo.countUnassignedMulti(schoolUuids));
        }

        counts.put("NO_HW", groupRepo.countGroupsWithoutHwMulti(schoolUuids, tUuid, now));
        counts.put("MISSING_ATTENDANCE", lessonRepo.countMissingAttendanceMulti(schoolUuids, tUuid, now));
        counts.put("AT_RISK", studentRepo.countAtRiskMulti(schoolUuids, tUuid));
        counts.put("OVER_DUE_COURSES", courseRepo.countOverdueCoursesMulti(schoolUuids, tUuid, now));

        return counts;
    }

    @Transactional
    public Page<?> getAttentionDetails(AttentionType type, Pageable pageable) {
        User user = userService.getCurrentUser();
        Collection<UUID> schoolUuids = userScopeService.getAuthorizedSchoolUuids();
        UUID tUuid = (user.getRole() == UserRole.TEACHER) ? user.getUuid() : null;
        Instant now = Instant.now();

        return switch (type) {
            case OVERDUE -> studentRepo.findOverdueMulti(schoolUuids, pageable)
                    .map(s -> new ResAttentionDetail(
                            s.getUuid(),
                            s.getUser().getFirstName() + " " + s.getUser().getLastName()
                    ));

            case UNASSIGNED -> studentRepo.findUnassignedMulti(schoolUuids, pageable)
                    .map(s -> new ResAttentionDetail(
                            s.getUuid(),
                            s.getUser().getFirstName() + " " + s.getUser().getLastName()
                    ));

            case NO_HW -> groupRepo.findGroupsWithoutHwMulti(schoolUuids, tUuid, now, pageable)
                    .map(g -> new ResAttentionDetail(
                            g.getUuid(),
                            g.getName()
                    ));

            case MISSING_ATTENDANCE -> lessonRepo.findMissingAttendanceMulti(schoolUuids, tUuid, now, pageable)
                    .map(l -> {
                        List<ResLessonDetails> allLessons = new ArrayList<>();
                        Page<CourseLesson> allByCourse = lessonRepo.findMissingByGroup(schoolUuids, l.getCourse().getGroup().getUuid(), now, pageable);
                        allByCourse.getContent().forEach(c -> {
                            allLessons.add(new ResLessonDetails(c));
                        });
                        return new ResAttentionDetail(
                                l.getCourse().getGroup().getUuid(),
                                l.getCourse().getGroup().getName(),
                                new ResGroupDetails(l.getCourse().getGroup(), allLessons)

                        );
                    });
            // Courses that ran past the school's configured course length but are still ACTIVE.
            case OVER_DUE_COURSES -> courseRepo.findOverdueCoursesMulti(schoolUuids, tUuid, now, pageable)
                    .map(c -> {
                        Group group = c.getGroup();
                        if (group == null) {
                            return new ResAttentionDetail(c.getUuid(), c.getName());
                        }
                        List<ResCourseDetails> groupCourses = courseRepo
                                .findOverdueCoursesByGroup(schoolUuids, group.getUuid(), now)
                                .stream().map(ResCourseDetails::new).toList();
                        return new ResAttentionDetail(
                                group.getUuid(),
                                group.getName(),
                                ResGroupDetails.forCourses(group, groupCourses)
                        );
                    });

            case AT_RISK -> {
                Page<Student> students = studentRepo.findAtRiskMulti(schoolUuids, tUuid, pageable);

                Map<UUID, ResAtRiskGroup> grouped = new LinkedHashMap<>();
                for (Student s : students.getContent()) {
                    // Multi-group: an at-risk student is listed under their primary (oldest) ongoing group.
                    // For a teacher, list them under the teacher's own group only.
                    Enrollment enrollment = enrollmentRepository
                            .findAllByStudent_User_UuidAndStatus(s.getUser().getUuid(), EnrollmentStatus.ONGOING)
                            .stream()
                            .filter(e -> tUuid == null
                                    || (e.getGroup() != null
                                        && e.getGroup().getTeacher() != null
                                        && tUuid.equals(e.getGroup().getTeacher().getUser().getUuid())))
                            .findFirst()
                            .orElse(null);
                    if (enrollment == null || enrollment.getGroup() == null) {
                        continue;
                    }

                    Group group = enrollment.getGroup();
                    ResAtRiskGroup target = grouped.computeIfAbsent(
                            group.getUuid(), k -> new ResAtRiskGroup(group));
                    target.getStudents().add(new ResAtRiskGroup.ResAtRiskStudent(
                            s.getUuid(),
                            s.getUser().getFirstName() + " " + s.getUser().getLastName()));
                }

                List<ResAtRiskGroup> content = new ArrayList<>(grouped.values());
                yield new PageImpl<>(content, pageable, students.getTotalElements());
            }
        };
    }
}
