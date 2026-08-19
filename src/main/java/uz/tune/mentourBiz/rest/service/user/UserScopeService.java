package uz.tune.mentourBiz.rest.service.user;

import uz.tune.mentourBiz.rest.domain.userManagement.user.User;
import uz.tune.mentourBiz.rest.payload.res.school.ResSchoolInfo;

import java.util.List;
import java.util.UUID;

public interface UserScopeService {

    UUID getCurrentUserSchoolUuid();
    List<UUID> getDirectorSchoolUuids();
    ResSchoolInfo getSchoolInfoForUser(User user);
    List<UUID> getAuthorizedSchoolUuids();
    UUID resolveSchoolUuid(UUID requestUuid);
}