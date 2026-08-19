package uz.tune.mentourBiz.rest.service.permission;

import uz.tune.mentourBiz.rest.enums.UserPermission;
import uz.tune.mentourBiz.rest.enums.UserRole;

import java.util.Set;

public interface PermissionService {
    Set<UserPermission> getPermissionsForRole(UserRole role);
}