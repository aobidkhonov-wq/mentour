package uz.tune.mentourBiz.rest.endpoint.util;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.tune.mentourBiz.base.BaseURI;
import uz.tune.mentourBiz.rest.enums.UserPermission;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.payload.req.permission.ReqPermissionCreate;
import uz.tune.mentourBiz.rest.payload.res.ResPermissionMatrix;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;
import uz.tune.mentourBiz.rest.service.PermissionManagementService;

import java.util.List;

@RestController
@RequestMapping(BaseURI.API1 + "/permissions")
@RequiredArgsConstructor
public class PermissionEndpoint {

    private final PermissionManagementService permissionManagementService;
    private final PermissionManagementService service;

    @PostMapping
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).PERMISSION_MANAGE)")
    public ResponseEntity<ResponseMessage> createPermission(@RequestBody ReqPermissionCreate request) {
        return ResponseEntity.ok(permissionManagementService.createPermission(request));
    }

    @GetMapping("/matrix")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).PERMISSION_MANAGE)")
    public ResponseEntity<List<ResPermissionMatrix>> getMatrix() {
        return ResponseEntity.ok(service.getPermissionMatrix());
    }

    @PostMapping("/toggle")
    @PreAuthorize("hasAuthority(T(uz.tune.mentourBiz.rest.enums.UserPermission).PERMISSION_MANAGE)")
    public ResponseEntity<ResponseMessage> toggle(
            @RequestParam UserRole role,
            @RequestParam UserPermission permission,
            @RequestParam boolean enabled) {
        return ResponseEntity.ok(service.togglePermission(role, permission, enabled));
    }
}