package uz.tune.mentourBiz.rest.service.impl;
import uz.tune.mentourBiz.rest.enums.MessageKey;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uz.tune.mentourBiz.exception.ValidationException;
import uz.tune.mentourBiz.rest.domain.userManagement.user.Permission;
import uz.tune.mentourBiz.rest.enums.UserPermission;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.payload.req.permission.ReqPermissionCreate;
import uz.tune.mentourBiz.rest.payload.res.ResPermissionMatrix;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.repository.user.PermissionRepo;
import uz.tune.mentourBiz.rest.service.PermissionManagementService;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionManagementServiceImpl implements PermissionManagementService {

    private final PermissionRepo permissionRepo;

    @Override
    public List<ResPermissionMatrix> getPermissionMatrix() {
        List<Permission> allPermissions = permissionRepo.findAll();

        return Arrays.stream(UserPermission.values()).map(up -> {
            ResPermissionMatrix row = new ResPermissionMatrix();
            row.setPermission(up);
            row.setDisplayName(formatName(up.name()));

            Map<UserRole, Boolean> access = new HashMap<>();
            for (UserRole role : UserRole.values()) {
                boolean hasAccess = allPermissions.stream()
                        .anyMatch(p -> p.getName() == up && p.getRole() == role);
                access.put(role, hasAccess);
            }
            row.setRoleAccess(access);
            return row;
        }).collect(Collectors.toList());
    }

    @Override
    public ResponseMessage togglePermission(UserRole role, UserPermission permission, boolean enabled) {
        Optional<Permission> existing = permissionRepo.findAllByRole(role).stream()
                .filter(p -> p.getName() == permission)
                .findFirst();

        if (enabled && existing.isEmpty()) {
            Permission p = new Permission();
            p.setName(permission);
            p.setRole(role);
            p.setDisplayName(formatName(permission.name()));
            permissionRepo.save(p);
        } else if (!enabled && existing.isPresent()) {
            permissionRepo.delete(existing.get());
        }

        return new ResponseMessage("Permission updated");
    }

    private String formatName(String name) {
        return Arrays.stream(name.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    @Override
    public ResponseMessage createPermission(ReqPermissionCreate request) {
        if (permissionRepo.existsByNameAndRole(request.getName(), request.getRole())) {
            throw new ValidationException(MessageKey.PERMISSION_EXISTS.getKey());
        }

        Permission newPermission = new Permission();
        newPermission.setName(request.getName());
        newPermission.setRole(request.getRole());
        newPermission.setDisplayName(formatPermissionName(request.getName()));

        permissionRepo.save(newPermission);

        return new ResponseMessage("Permission '" + request.getName() + "' successfully created for role '" + request.getRole() + "'.");
    }

    private String formatPermissionName(UserPermission permission) {
        return Arrays.stream(permission.name().split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}