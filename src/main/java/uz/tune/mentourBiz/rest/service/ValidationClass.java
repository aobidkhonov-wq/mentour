package uz.tune.mentourBiz.rest.service;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.Course;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Teacher;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.repository.SchoolDirectorRepo;
import uz.tune.mentourBiz.rest.repository.user.TeacherRepository;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ValidationClass {

    private final TeacherRepository teacherRepository;
    private final UserScopeService userScopeService;
    private final SchoolDirectorRepo schoolDirectorRepo;

    public void validateUserAccess(Course course, User currentUser) {
        if (currentUser.getRole().equals(UserRole.SYS_ADMIN)) return;

        if (currentUser.getRole().equals(UserRole.SCHOOL_DIRECTOR)) {
            if (course.getSchool().getOrganization().equals(schoolDirectorRepo.findByUser(currentUser).get().getOrganization())) return;
        }

        if (currentUser.getRole().equals(UserRole.MENTOR)) {
            if (!course.getMentor().getUser().equals(currentUser)) throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
        } else if (currentUser.getRole().equals(UserRole.TEACHER)) {
            Teacher teacher = teacherRepository.findByUser_Uuid(currentUser.getUuid())
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));
            if (!course.getGroup().getTeacher().equals(teacher)) throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
        } else {
            UUID schoolUuid = userScopeService.getCurrentUserSchoolUuid();
            if (!course.getSchool().getUuid().equals(schoolUuid)) throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
        }
    }
}
