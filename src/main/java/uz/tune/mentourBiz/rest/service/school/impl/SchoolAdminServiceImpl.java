package uz.tune.mentourBiz.rest.service.school.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uz.tune.mentourBiz.exception.EntityNotFoundException;
import uz.tune.mentourBiz.rest.domain.userManagement.user.SchoolAdmin;
import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.enums.MessageKey;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.enums.UserStatus;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.payload.res.user.ResSchoolAdminOne;
import uz.tune.mentourBiz.rest.repository.user.SchoolAdminRepo;
import uz.tune.mentourBiz.rest.service.helper.AuthToViewEntity;
import uz.tune.mentourBiz.rest.service.user.SchoolAdminService;
import uz.tune.mentourBiz.rest.service.user.UserScopeService;
import uz.tune.mentourBiz.rest.service.user.UserService;
import uz.tune.mentourBiz.rest.service.util.MessageSingleton;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SchoolAdminServiceImpl implements SchoolAdminService {

    private final SchoolAdminRepo schoolAdminRepo;
    private final MessageSingleton messageSingleton;
    private final UserService userService;
    private final UserScopeService userScopeService;
    private final AuthToViewEntity authToViewEntity;

    @Override
    public ResSchoolAdminOne getOne(UUID uuid) {
        SchoolAdmin admin = schoolAdminRepo.findByUserUuid(uuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.SCHOOL_ADMIN_NOT_FOUND.getKey()));

        authToViewEntity.authorizeActionUponSchoolBroadAccess(admin.getSchool());

        return new ResSchoolAdminOne(admin);
    }

    @Override
    public Page<ResSchoolAdminOne> getAll(Pageable pageable, String schoolUuid) {
        User currentUser = userService.getCurrentUser();

        // 1. SYS_ADMIN Global View Logic
        if (currentUser.getRole() == UserRole.SYS_ADMIN && schoolUuid == null) {
            return schoolAdminRepo.findAllByUser_Status(UserStatus.ACTIVE, pageable)
                    .map(ResSchoolAdminOne::new);
        }

        // 2. Filtered/Scoped Logic (for Directors, Admins, or SysAdmin with specific filter)
        UUID inputUuid = (schoolUuid != null) ? UUID.fromString(schoolUuid) : null;
        UUID resolvedId = userScopeService.resolveSchoolUuid(inputUuid);

        Collection<UUID> authorizedUuids;
        if (resolvedId != null) {
            authorizedUuids = List.of(resolvedId);
        } else {
            authorizedUuids = userScopeService.getAuthorizedSchoolUuids();
        }

        return schoolAdminRepo.findAllBySchool_UuidInAndUser_Status(authorizedUuids, UserStatus.ACTIVE, pageable)
                .map(ResSchoolAdminOne::new);
    }

    @Override
    @Transactional
    public ResponseMessage deleteSchoolAdmin(UUID adminUuid) {
        SchoolAdmin admin = schoolAdminRepo.findByUserUuid(adminUuid)
                .orElseThrow(() -> new EntityNotFoundException(MessageKey.USER_NOT_FOUND.getKey()));

        authToViewEntity.authorizeActionUponSchoolBroadAccess(admin.getSchool());

        userService.deleteUser(admin.getUser().getUuid(), null);
        return new ResponseMessage("School Administrator account deactivated.");
    }
}