package uz.tune.mentourBiz.rest.service.helper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.tune.mentourBiz.config.GlobalVar;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.rest.domain.StudentParentContact;
import uz.tune.mentourBiz.rest.domain.schoolManagement.course.Course;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.group.Enrollment;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Teacher;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.EnrollmentStatus;
import uz.tune.mentourBiz.rest.enums.MessageKey;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.repository.SchoolDirectorRepo;
import uz.tune.mentourBiz.rest.repository.StudentParentContactRepository;
import uz.tune.mentourBiz.rest.repository.group.enrollment.EnrollmentRepository;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.repository.user.TeacherRepository;
import uz.tune.mentourBiz.rest.service.user.impl.UserScopeServiceImpl;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthToViewEntity {
    private final StudentRepo studentRepo;
    private final UserScopeServiceImpl userScopeService;
    private final TeacherRepository teacherRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SchoolDirectorRepo schoolDirectorRepo;
    private final StudentParentContactRepository studentParentContactRepository;

    private User getCurrentUser() {
        return GlobalVar.getUserDetails().getUser();
    }

    public void authorizeActionUponTeacher(Teacher teacher) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole().equals(UserRole.SYS_ADMIN)) return;

        if (currentUser.getRole().equals(UserRole.SCHOOL_DIRECTOR)) {
            if (teacher.getSchool().getOrganization().equals(schoolDirectorRepo.findByUser(currentUser).get().getOrganization()))
                return;
        }

        List<UUID> authorizedUuids = userScopeService.getAuthorizedSchoolUuids();
        if (authorizedUuids == null || !authorizedUuids.contains(teacher.getSchool().getUuid())) {
            throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
        }
    }

    public void authorizeActionUponStudent(Student student) {
        User currentUser = getCurrentUser();
        if (currentUser.getRole().equals(UserRole.SYS_ADMIN)) return;

        if (currentUser.getRole().equals(UserRole.SCHOOL_DIRECTOR)) {
            if (student.getSchool().getOrganization().equals(schoolDirectorRepo.findByUser(currentUser).get().getOrganization()))
                return;
        }

        if (currentUser.getRole().equals(UserRole.TEACHER)) {
            // Multi-group: authorized if ANY ongoing enrollment is taught by this teacher.
            boolean teaches = enrollmentRepository
                    .findAllByStudent_User_UuidAndStatus(student.getUser().getUuid(), EnrollmentStatus.ONGOING)
                    .stream()
                    .anyMatch(e -> e.getGroup().getTeacher() != null
                            && e.getGroup().getTeacher().getUser().getUuid().equals(currentUser.getUuid()));
            if (teaches) return;
        }

        List<UUID> authorizedUuids = userScopeService.getAuthorizedSchoolUuids();
        if (authorizedUuids == null || !authorizedUuids.contains(student.getSchool().getUuid())) {
            throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
        }
    }

    public void authorizeUponCourse(User user, Course course) {
        if (user.getRole().equals(UserRole.SYS_ADMIN)) return;

        if (user.getRole().equals(UserRole.SCHOOL_DIRECTOR)) {
            UUID dirOrgUuid = schoolDirectorRepo.findByUser(user)
                    .map(sd -> sd.getOrganization() != null ? sd.getOrganization().getUuid() : null)
                    .orElse(null);

            UUID courseOrgUuid = (course.getSchool().getOrganization() != null)
                    ? course.getSchool().getOrganization().getUuid() : null;

            if (dirOrgUuid != null && dirOrgUuid.equals(courseOrgUuid)) return;
            throw new PermissionForbidden(MessageKey.ORG_MISMATCH.getKey());
        }

        if (user.getRole().equals(UserRole.SCHOOL_ADMIN)) {
            UUID adminSchoolUuid = userScopeService.getCurrentUserSchoolUuid();
            if (course.getSchool().getUuid().equals(adminSchoolUuid)) return;
        }

        if (user.getRole().equals(UserRole.TEACHER)) {
            if (course.getGroup().getTeacher().getUser().getUuid().equals(user.getUuid())) return;
        }

        if (user.getRole().equals(UserRole.STUDENT)) {
            Student student = studentRepo.findByUser(user)
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

            boolean isEnrolled = enrollmentRepository.existsByStudentAndGroup(student, course.getGroup());

            if (isEnrolled) return; // Authorized
        }

        if (UserRole.PARENT.equals(user.getRole())) {
            List<StudentParentContact> allByPhoneNumberAndIsActiveTrue = studentParentContactRepository.findAllByPhoneNumberAndIsActiveTrue(user.getUsername());
            if (allByPhoneNumberAndIsActiveTrue.isEmpty()) return;
            if (allByPhoneNumberAndIsActiveTrue.get(0) == null) return;
            Student student = allByPhoneNumberAndIsActiveTrue.get(0).getStudent();

            boolean isEnrolled = enrollmentRepository.existsByStudentAndGroup(student, course.getGroup());

            if (isEnrolled) return; // Authorized
        }

        throw new PermissionForbidden(MessageKey.UNAUTHORIZED.getKey());
    }

    public void authorizeActionUponSchoolBroadAccess(School school) {
        User user = getCurrentUser();
        if (user.getRole().equals(UserRole.SYS_ADMIN)) return;

        if (user.getRole().equals(UserRole.SCHOOL_DIRECTOR)) {
            UUID dirOrgUuid = schoolDirectorRepo.findByUser(user)
                    .map(sd -> sd.getOrganization() != null ? sd.getOrganization().getUuid() : null)
                    .orElse(null);

            UUID targetOrgUuid = (school.getOrganization() != null) ? school.getOrganization().getUuid() : null;

            if (dirOrgUuid != null && dirOrgUuid.equals(targetOrgUuid)) return;
            throw new PermissionForbidden(MessageKey.ORG_MISMATCH.getKey());
        }

        List<UUID> authorizedUuids = userScopeService.getAuthorizedSchoolUuids();
        if (authorizedUuids != null && authorizedUuids.contains(school.getUuid())) return;

        throw new PermissionForbidden(MessageKey.SCHOOL_CONTEXT_REQUIRED.getKey());
    }


    public void authorizeActionUponSchool(School school) {
        authorizeActionUponSchoolBroadAccess(school);
    }
}