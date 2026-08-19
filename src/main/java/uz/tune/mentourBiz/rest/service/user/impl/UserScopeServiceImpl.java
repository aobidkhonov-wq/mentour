package uz.tune.mentourBiz.rest.service.user.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.tune.mentourBiz.config.GlobalVar;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.exception.PermissionForbidden;
import uz.tune.mentourBiz.rest.domain.SchoolSubscription;
import uz.tune.mentourBiz.rest.domain.schoolManagement.school.School;
import uz.tune.mentourBiz.rest.domain.userManagement.user.SchoolAdmin;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Student;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Teacher;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.MessageKey;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.payload.res.ResSchoolSubscription;
import uz.tune.mentourBiz.rest.payload.res.school.ResSchoolInfo;
import uz.tune.mentourBiz.rest.repository.SchoolDirectorRepo;
import uz.tune.mentourBiz.rest.repository.SchoolSubscriptionRepo;
import uz.tune.mentourBiz.rest.repository.school.SchoolRepo;
import uz.tune.mentourBiz.rest.repository.student.StudentRepo;
import uz.tune.mentourBiz.rest.repository.user.SchoolAdminRepo;
import uz.tune.mentourBiz.rest.repository.user.TeacherRepository;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.util.MessageSingleton;
import uz.tune.mentourBiz.utils.CoreUtils;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

public class UserScopeServiceImpl implements UserScopeService {

    private final SchoolAdminRepo schoolAdminRepo;
    
    private final MessageSingleton messageSingleton;
    private final StudentRepo studentRepo;
    private final TeacherRepository teacherRepo;
    private final SchoolSubscriptionRepo schoolSubscriptionRepo;
    private final SchoolDirectorRepo schoolDirectorRepo;
    private final SchoolRepo schoolRepo;


    @Override
    public UUID resolveSchoolUuid(UUID requestUuid) {
        User user = GlobalVar.getUserDetails().getUser();

        if (user.getRole().equals(UserRole.SYS_ADMIN)) return requestUuid;

        if (user.getRole().equals(UserRole.SCHOOL_DIRECTOR)) {
            // 1. If Director provided a UUID (param), verify they own it
            if (requestUuid != null) {
                List<UUID> owned = getDirectorSchoolUuids();
                if (owned.contains(requestUuid)) return requestUuid;
                throw new PermissionForbidden(MessageKey.ORG_MISMATCH.getKey());
            }

            // 2. If no param, check if they have a school selected in the UI (Header)
            String headerScope = GlobalVar.getSchoolScope();
            if (CoreUtils.isPresent(headerScope)) {
                UUID targetUuid = UUID.fromString(headerScope);
                // Re-verify header school belongs to Director's Org
                List<UUID> owned = getDirectorSchoolUuids();
                if (owned.contains(targetUuid)) return targetUuid;
                throw new PermissionForbidden(MessageKey.SCHOOL_CONTEXT_REQUIRED.getKey());
            }
            return null;
        }

        return getCurrentUserSchoolUuid();
    }
    @Override
    public UUID getCurrentUserSchoolUuid() {
        User currentUser = GlobalVar.getUserDetails().getUser();
        String schoolScope = GlobalVar.getSchoolScope();

        if (currentUser.getRole().equals(UserRole.SCHOOL_DIRECTOR)) {
            if (CoreUtils.isEmpty(schoolScope)) return null;
            UUID targetUuid = UUID.fromString(schoolScope);
            return schoolDirectorRepo.findByUser(currentUser)
                    .map(director -> {
                        boolean belongs = schoolRepo.findByUuid(targetUuid)
                                .map(s -> s.getOrganization() != null && s.getOrganization().equals(director.getOrganization()))
                                .orElse(false);
                        if (!belongs) throw new PermissionForbidden(MessageKey.SCHOOL_CONTEXT_REQUIRED.getKey());
                        return targetUuid;
                    })
                    .orElseThrow(() -> new EntityNotFoundException(MessageKey.DIRECTOR_NOT_FOUND.getKey()));
        }

        return switch (currentUser.getRole()) {
            case SCHOOL_ADMIN -> schoolAdminRepo.findByUser(currentUser).map(SchoolAdmin::getSchool).map(School::getUuid).orElse(null);
            case STUDENT -> studentRepo.findByUser(currentUser).map(Student::getSchool).map(School::getUuid).orElse(null);
            case TEACHER -> teacherRepo.findByUser(currentUser).map(Teacher::getSchool).map(School::getUuid).orElse(null);
            default -> null;
        };
    }

    @Override
    public List<UUID> getAuthorizedSchoolUuids() {
        User user = GlobalVar.getUserDetails().getUser();

        // Repositories interpret NULL as "No filter" (Show every school on the platform).
        if (user.getRole().equals(UserRole.SYS_ADMIN)) {
            return null;
        }

        // 2. School Director: Return ONLY schools in their Org.
        if (user.getRole().equals(UserRole.SCHOOL_DIRECTOR)) {
            List<UUID> ownedSchools = getDirectorSchoolUuids();
            // If not, return an empty list (this forces SQL to return 0 results, preventing leakage)
            return ownedSchools != null ? ownedSchools : Collections.emptyList();
        }

        UUID singleSchool = getCurrentUserSchoolUuid();
        return (singleSchool != null) ? List.of(singleSchool) : Collections.emptyList();
    }

    @Override
    public List<UUID> getDirectorSchoolUuids() {
        User currentUser = GlobalVar.getUserDetails().getUser();
        if (!currentUser.getRole().equals(UserRole.SCHOOL_DIRECTOR)) return List.of();
        return schoolDirectorRepo.findByUser(currentUser)
                .map(d -> d.getOrganization().getSchools().stream()
                        .map(School::getUuid)
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    @Override
    public ResSchoolInfo getSchoolInfoForUser(User user) {
        School school = switch (user.getRole()) {
            case SCHOOL_ADMIN -> schoolAdminRepo.findByUser(user).map(SchoolAdmin::getSchool).orElse(null);
            case STUDENT -> studentRepo.findByUser(user).map(Student::getSchool).orElse(null);
            case TEACHER -> teacherRepo.findByUser(user).map(Teacher::getSchool).orElse(null);
            default -> null;
        };

        if (school == null) return null;

        SchoolSubscription sub = schoolSubscriptionRepo.findBySchool_Uuid(school.getUuid()).orElse(null);
        return new ResSchoolInfo(school, (sub != null) ? new ResSchoolSubscription(school, sub) : null);
    }
}