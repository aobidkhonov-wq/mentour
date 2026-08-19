package uz.tune.mentourBiz.rest.service.permission.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Permission;
import uz.tune.mentourBiz.rest.enums.UserPermission;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.repository.user.PermissionRepo;
import uz.tune.mentourBiz.rest.service.permission.PermissionService;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepo permissionRepo;

    @Override
    public Set<UserPermission> getPermissionsForRole(UserRole role) {
        return permissionRepo.findAllByRole(role).stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());
    }
}