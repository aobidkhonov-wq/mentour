package uz.tune.mentourBiz.rest.service;

import uz.tune.mentourBiz.rest.enums.UserPermission;
import uz.tune.mentourBiz.rest.enums.UserRole;
import uz.tune.mentourBiz.rest.payload.req.permission.ReqPermissionCreate;
import uz.tune.mentourBiz.rest.payload.res.ResPermissionMatrix;
import uz.tune.mentourBiz.rest.payload.res.ResponseMessage;

import java.util.List;

public interface PermissionManagementService {
    List<ResPermissionMatrix> getPermissionMatrix();
    ResponseMessage togglePermission(UserRole role, UserPermission permission, boolean enabled);
    ResponseMessage createPermission(ReqPermissionCreate request);
}
