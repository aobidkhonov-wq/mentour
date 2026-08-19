package uz.tune.mentourBiz.rest.repository.user;

import uz.tune.mentourBiz.base.BaseRepository;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Permission;
import uz.tune.mentourBiz.rest.enums.UserPermission;
import uz.tune.mentourBiz.rest.enums.UserRole;

import java.util.List;

public interface PermissionRepo extends BaseRepository<Permission> {

    List<Permission> findAllByRole(UserRole role);
    boolean existsByNameAndRole(UserPermission name, UserRole role);
}
