package uz.tune.mentourBiz.rest.service.lessons.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.Course;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.CourseLesson;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.CourseStatus;
import uz.tune.mentourBiz.rest.enums.EnrollmentStatus;
import uz.tune.mentourBiz.rest.enums.LessonStatus;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.payload.res.lesson.ResUpcomingLesson;
import uz.tune.mentourBiz.rest.repository.course.CourseLessonRepo;
import uz.tune.mentourBiz.rest.repository.course.CourseRepo;
import uz.tune.mentourBiz.rest.repository.group.GroupRepository;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentRepository;
import uz.tune.mentourBiz.rest.repository.payment.PaymentOrderRepo;
import uz.tune.mentourBiz.rest.repository.schedule.GroupScheduleRepository;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.service.lessons.DashboardService;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;
import uz.tune.mentourBiz.rest.service.util.MessageSingleton;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final CourseLessonRepo courseLessonRepo;
    private final UserService userService;
    private final UserScopeService userScopeService;
    private final MessageSingleton messageSingleton;
    private final PaymentOrderRepo paymentOrderRepo;
    private final StudentRepo studentRepo;
    private final GroupRepository groupRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final GroupScheduleRepository groupScheduleRepository;
    private final CourseRepo courseRepo;

    @Override
    public Page<ResUpcomingLesson> getUpcomingLessons(Pageable pageable, List<LessonStatus> status, UUID schoolId) {
        User currentUser = userService.getCurrentUser();
        List<LessonStatus> statuses = (status == null)
                ? List.of(LessonStatus.NEW, LessonStatus.STARTED, LessonStatus.STUDENT_APP)
                : status;

        UUID resolvedId = userScopeService.resolveSchoolUuid(schoolId);
        Collection<UUID> authorizedUuids;

        if (resolvedId != null) {
            authorizedUuids = List.of(resolvedId);
        } else {
            authorizedUuids = userScopeService.getAuthorizedSchoolUuids();
        }

        Page<CourseLesson> lessonsPage;
        if (currentUser.getRole().equals(UserRole.SYS_ADMIN) && schoolId == null) {
            lessonsPage = courseLessonRepo.findAllByStatusIn(statuses, pageable);
        } else if (currentUser.getRole().equals(UserRole.STUDENT)) {
            List<UUID> targetCourseIds = enrollmentRepository.findAllByStudent_User_UuidAndStatus(
                            currentUser.getUuid(), EnrollmentStatus.ONGOING)
                    .stream()
                    .map(e -> courseRepo.findAllByGroupAndStatus(e.getGroup(), CourseStatus.ACTIVE))
                    .flatMap(List::stream)
                    .map(Course::getUuid)
                    .toList();
            if (targetCourseIds.isEmpty()) return Page.empty(pageable);
            lessonsPage = courseLessonRepo.findAllByCourse_UuidInAndStatusIn(targetCourseIds, statuses, pageable);
        } else {
            lessonsPage = courseLessonRepo.findAllByCourse_School_UuidInAndStatusIn(authorizedUuids, statuses, pageable);
        }

        return lessonsPage.map(lesson -> new ResUpcomingLesson(lesson, currentUser));
    }
}