package uz.tune.mentourBiz.rest.service.util.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.tune.mentourBiz.config.logger.Logger;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.Course;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.CourseStatus;
import uz.tune.mentourBiz.rest.enums.SchoolStatus;
import uz.tune.mentourBiz.rest.enums.UserStatus;
import uz.tune.mentourBiz.rest.repository.course.CourseRepo;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.repository.user.SchoolAdminRepo;
import uz.tune.mentourBiz.rest.repository.user.UserRepo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduledCleanupServiceImpl {

    private final UserRepo userRepo;
    
    
    private final SchoolAdminRepo schoolAdminRepo;
    private final StudentRepo studentRepo;
    private final SchoolRepo schoolRepo;
    private final CourseRepo courseRepo;

//    @Scheduled(cron = "0 0 1 * * *") // Runs every day at 1 AM UTC
    @Transactional
    public void cleanupSoftDeletedEntities() {
        Logger.logInfo("Scheduled cleanup task started.");
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);

        List<User> usersToDelete = userRepo.findAllByStatusAndUpdatedAtBefore(UserStatus.BLOCKED, sevenDaysAgo);
        if (!usersToDelete.isEmpty()) {
            List<Long> userIds = usersToDelete.stream().map(User::getId).collect(Collectors.toList());
            schoolAdminRepo.deleteAllByUserIdIn(userIds);
            studentRepo.deleteAllByUserIdIn(userIds);

            userRepo.deleteAll(usersToDelete);
            Logger.logInfo("Hard deleted " + usersToDelete.size() + " users older than 7 days.");
        }

        List<Course> coursesToDelete = courseRepo.findAllByStatusAndUpdatedAtBefore(CourseStatus.DELETED, sevenDaysAgo);
        if(!coursesToDelete.isEmpty()) {
            courseRepo.deleteAll(coursesToDelete);
            Logger.logInfo("Hard deleted " + coursesToDelete.size() + " courses older than 7 days.");
        }

        List<School> schoolsToDelete = schoolRepo.findAllByStatusAndUpdatedAtBefore(SchoolStatus.DELETED, sevenDaysAgo);
        if(!schoolsToDelete.isEmpty()) {
            schoolRepo.deleteAll(schoolsToDelete);
            Logger.logInfo("Hard deleted " + schoolsToDelete.size() + " schools older than 7 days.");
        }

        Logger.logInfo("Scheduled cleanup task finished.");
    }
}