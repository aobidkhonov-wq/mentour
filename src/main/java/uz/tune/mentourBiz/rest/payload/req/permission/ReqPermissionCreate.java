package uz.tune.mentourBiz.rest.payload.req.permission;

import lombok.Getter;
import lombok.Setter;
import uz.tune.mentourBiz.rest.enums.UserPermission;
import uz.tune.mentourBiz.rest.enums.UserRole;

@Getter
@Setter
public class ReqPermissionCreate {
    private UserPermission name;
    private UserRole role;
}
